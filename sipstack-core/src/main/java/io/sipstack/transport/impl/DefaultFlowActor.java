package io.sipstack.transport.impl;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.address.Address;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.header.FromHeader;
import io.pkts.packet.sip.header.ToHeader;
import io.sipstack.actor.ActorSupport;
import io.sipstack.actor.Cancellable;
import io.sipstack.config.FlowConfiguration;
import io.sipstack.config.KeepAliveMethodConfiguration;
import io.sipstack.config.SipOptionsPingConfiguration;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.event.*;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowId;
import io.sipstack.transport.FlowState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

/**
 *
 * Implements the following state machine:
 *
 *     http://www.gliffy.com/go/publish/image/9427857/L.png
 *
 * @author jonas@jonasborjesson.com
 */
public class DefaultFlowActor extends ActorSupport<IOEvent, FlowState> implements FlowActor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFlowActor.class);

    private final FlowConfiguration config;

    private final TransportLayerConfiguration transportLayerConfiguration;

    private final Connection connection;

    /**
     * The time when the last message was processed and is used for
     * keeping track of whether e.g. a PING is necessary to send out.
     */
    private long lastMessageProcessed;

    private final Clock clock;

    /**
     * If we are using SIP OPTIONS as a ping mechanism, we need to save
     * the transaction id so we can match any potential responses.
     */
    private TransactionId optionsTransaction;

    /**
     * A flow is always only mapped to a single transport and as such, there is
     * only one of the keep-alive method configuration that actually matters
     * to the flow so figure that out at construction time so we don't have to
     * keep looking it up.
     */
    private final KeepAliveMethodConfiguration keepAliveMethodConfig;

    private final FlowId flowId;

    /**
     * If we are not in the ACTIVE ping mode then we will schedule a
     * maximum lifetime timer so if we haven't received any traffic
     * when this timer fires, we will kill the flow.
     */
    private Optional<Cancellable> lifeTimeTimer = Optional.empty();

    /**
     * There are several other timers we use apart from the life time timer above. These
     * other timeouts has to do with e.g. waiting for a ping, waiting for initial anything
     * when we are in the READY state. We would only ever have one of these timers going
     * at any given point in time so therefore it is enough with one variable.
     *
     * Note, the {@link DefaultFlowActor#lifeTimeTimer} and the timer for the READY state
     * may be running at the same time so therefore we need two.
     */
    private Optional<Cancellable> timeoutTimer = Optional.empty();

    protected DefaultFlowActor(final TransportLayerConfiguration transportConfig,
                               final FlowId flowId,
                               final Connection connection,
                               final Clock clock) {
        super(connection.id().toString(), FlowState.INIT, FlowState.CLOSED, FlowState.values());
        this.transportLayerConfiguration = transportConfig;
        this.config = transportConfig.getFlow();
        this.connection = connection;
        this.flowId = flowId;
        this.clock = clock;

        this.keepAliveMethodConfig =
                config.getKeepAliveConfiguration().getKeepAliveMethodConfiguration(connection.getTransport());

        always(this::alwaysExecute);

        when(FlowState.INIT, this::onInit);

        onEnter(FlowState.READY, this::onEnterReady);
        when(FlowState.READY, this::onReady);
        onExit(FlowState.READY, this::onExitReady);

        onEnter(FlowState.ACTIVE, this::onEnterActive);
        when(FlowState.ACTIVE, this::onActive);
        onExit(FlowState.ACTIVE, this::onExitActive);

        // Using guards we could just write this
        // when(FlowState.ACTIVE, e -> e.isSipTimerTimeout2(), this::onSipTimerTimeout2InActive);

        // Setup the PING state only if we are in active ping mode.
        // Which type of ping we will do is also dependent on configuration
        // so adjust the implementation based on that.
        if (config.isPingModeActive()) {
            if (keepAliveMethodConfig.useDblCrlf()) {
                onEnter(FlowState.PING, this::onEnterDblCRLFPing);
                when(FlowState.PING, this::onDblCRLFPing);
                onExit(FlowState.PING, this::onExitDblCRLFPing);
            } else if (keepAliveMethodConfig.useSipOptions()) {
                onEnter(FlowState.PING, this::onEnterSipOptionsPing);
                when(FlowState.PING, this::onSipOptionsPing);
                onExit(FlowState.PING, this::onExitSipOptionsPing);
            } else if (keepAliveMethodConfig.useStun()) {
                logWarn("We are not able to use STUN as a ping mechanism for the time being");
            }
        }

        onEnter(FlowState.CLOSING, this::onEnterClosing);
        when(FlowState.CLOSING, this::onClosing);
        onExit(FlowState.CLOSING, this::onExitClosing);

        onEnter(FlowState.CLOSED, this::onEnterClosed);
        when(FlowState.CLOSED, this::onClosed);
    }

    // =====================
    // === Always execute the following for all events
    // =====================
    private void alwaysExecute(final IOEvent event) {
        if (event.isSipMessageIOEvent() || event.isSipMessageBuilderIOEvent()) {
            this.lastMessageProcessed = clock.getCurrentTimeMillis();
        }
    }

    // =====================
    // === Init State
    // =====================

    private void onInit(final IOEvent event) {

        // if we are not in active ping mode we need to schedule the max lifetime
        // timer of the flow.
        if (!config.isPingModeActive()) {
            lifeTimeTimer = Optional.of(ctx().scheduler().schedule(SipTimer.Timeout, config.getTimeout()));
        }

        if (event.isSipMessageIOEvent()) {
            processSipMessageEvent(event.toSipMessageIOEvent());
            become(FlowState.ACTIVE, "SIP message received");
        } else if (event.isConnectionIOEvent()) {
            final ConnectionIOEvent connectionEvent = event.toConnectionIOEvent();
            if (connectionEvent.isConnectionOpenedIOEvent()) {
                become(FlowState.READY, "Connection opened");
            }
        } else {
            unhandled(event);
        }
    }

    // =====================
    // === Ready State
    // =====================
    private void onEnterReady(final IOEvent event) {
        timeoutTimer = Optional.of(ctx().scheduler().schedule(SipTimer.Timeout1, config.getInitialIdleTimeout()));
    }

    private void onExitReady(final IOEvent event) {
        timeoutTimer.ifPresent(t -> t.cancel());
    }

    private void onReady(final IOEvent event) {
        if (event.isSipMessageIOEvent()) {
            processSipMessageEvent(event.toSipMessageIOEvent());
            become(FlowState.ACTIVE, "SIP message received");
        } else if (event.isSipTimerTimeout()) {
            // this is the max life-time timer. When in READY
            // state that means we didn't get anything for a long time.
            // This shouldn't really happen since the initial idle timeout
            // should take care of it.
            become(FlowState.CLOSING, "Maximum flow life-time reached");
        } else if (event.isSipTimerTimeout1()) {
            // we didn't get anything across this flow so kill it.
            // someone is probably establishing TCP connections
            // in an effort to attack us
            become(FlowState.CLOSING, "Nothing received across the flow");
        } else if (event.isConnectionInactiveIOEvent()) {
            // guess someone just connected and then tore down the connection again
            become(FlowState.CLOSING, "Connection closed by peer");
        } else if (event.isPingMessageIOEvent()) {
            if (config.isPingModeActive()) {
                sendPong();
            }
            become(FlowState.ACTIVE, "Ping received");
        } else if (event.isPongMessageIOEvent()) {
            // just consume. Probably won't happen. Guess if someone sends
            // a single CRLF then that would be interpreted as a PONG message
            // and sent our way
            become(FlowState.ACTIVE, "Pong received");
        }
    }

    /**
     * TODO: is this true?
     *
     * Every time we receive a SIP message we will have to determine
     * if this is an options request/response and if so, are we configured
     * to use options as a ping mechanism. If we are, then we will not
     * forward the request/response but consume the message as a piong.
     *
     * @param event
     */
    private void processSipMessageEvent(final SipMessageIOEvent event) {
        if (!config.isPingModeOff() && event.message().isOptions() && keepAliveMethodConfig.acceptSipOptions()) {
            if (config.isPingModeActive()) {
                sendPong();
            }
        } else {
            ctx().forward(event);
        }
    }

    // =====================
    // === Active State
    // =====================
    private void onEnterActive(final IOEvent event) {

        // In active mode we will schedule a timer to ensure we get
        // keep-alive traffic and if not, we will issue keep-alive
        // traffic ourselves. SipTimer.Timeout2 is used for this purpose
        if (config.isPingModeActive()) {
            final Duration duration = this.config.getKeepAliveConfiguration().getIdleTimeout();
            timeoutTimer = Optional.of(ctx().scheduler().schedule(SipTimer.Timeout2, duration));
        }
    }

    private void onExitActive(final IOEvent event) {
        timeoutTimer.ifPresent(t -> t.cancel());
    }

    /**
     * TOOD: update diagram with when we receive a PONG
     * of some sort we should just consume it if.
     *
     * @param event
     */
    private void onActive(final IOEvent event) {
        if (event.isSipMessageBuilderIOEvent()) {
            forwardBuilderEvent(event.toSipMessageBuilderIOEvent());
        } else if (event.isSipMessageIOEvent()) {
            if (isOutstandingSipOptionsPong(event.toSipMessageIOEvent())) {
                // absorb the 200 OK to the options and clear out the options transaction
                optionsTransaction = null;
            } else {
                ctx().forward(event);
            }
        } else if (event.isPingMessageIOEvent()) {
            become(FlowState.PING);
        } else if (event.isPongMessageIOEvent()) {
            // just consume
        } else if (event.isSipTimerTimeout()) {
            onFlowLifeTimeTimerTimeout(event);
        } else if (event.isSipTimerTimeout2()) {
            onSipTimerTimeout2InActive(event);
        } else {
            unhandled(event);
        }

        // TODO: need some close event. Probably should add that to the Flow itself.
        // something like flow.kill()
    }

    private void onFlowLifeTimeTimerTimeout(final IOEvent event) {
        final Duration duration = this.config.getTimeout();
        final long diffInSeconds = (clock.getCurrentTimeMillis() - lastMessageProcessed) / 1000;
        final Duration diff = duration.minusSeconds(diffInSeconds);
        if (diff.isNegative() || diff.isZero()) {
            become(FlowState.CLOSING, "No traffic received in " + diffInSeconds + " seconds. Closing flow");
        } else {
            ctx().scheduler().schedule(SipTimer.Timeout, diff);
        }
    }

    private void onSipTimerTimeout2InActive(final IOEvent event) {
        final Duration duration = this.config.getKeepAliveConfiguration().getIdleTimeout();
        final Duration diff = duration.minusMillis(clock.getCurrentTimeMillis() - lastMessageProcessed);
        if (diff.isNegative() || diff.isZero()) {
            become(FlowState.PING, "Idle timer fired");
        } else {
            ctx().scheduler().schedule(SipTimer.Timeout2, diff);
        }
    }

    /**
     * We ONLY receive {@link SipMessageBuilderIOEvent} from layers on top of us
     * so we know for sure that this is an event that is about to get written
     * to the socket. So, when we get one of these builder events we are being
     * asked to fill out the Via- and Contact-headers with the appropriate values
     * and really all we do is to just register another lambda to be executed when
     * the message eventually is built. Hence, the flow is NOT the one actually
     * building.
     *
     * Also, the flow will NOT push a Via-header so if there isn't one there then
     * there won't be one going out either. Same goes for the Contact-header.
     * The responsibility of adding the Via is on the layers above the flow, which
     * typically will be the transaction layer.
     *
     * @param event
     */
    private void forwardBuilderEvent(final SipMessageBuilderIOEvent event) {
        final SipMessage.Builder<? extends SipMessage> builder = event.getBuilder();
        builder.onTopMostViaHeader(v -> {
            v.withPort(connection.getLocalPort());
            v.withHost(connection.getLocalIpAddress());
            v.withTransport(connection.getTransport().toUpperCaseBuffer());

            if (builder.isSipRequestBuilder() && transportLayerConfiguration.isPushRPort()) {
                // only set the rport if it has been configured
                v.withRPortFlag();
            } else if (builder.isSipResponseBuilder()) {
                v.withReceived(connection.getRemoteIpAddress());

                // set the value of the rport if the rport already exists or
                // we have been configured to push it no matter what.
                if (v.hasRPort() || transportLayerConfiguration.getForceRPort()) {
                    v.withRPort(connection.getRemotePort());
                }
            }
        });

        builder.onContactHeader(c -> {
            final SipURI vip = connection.getVipAddress().orElse(null);
            final Buffer host = vip != null ? vip.getHost() : connection.getLocalIpAddressAsBuffer();
            c.withHost(host);
            c.withTransport(connection.getTransport().toBuffer());

            // don't stamp the port if the port is the default one for the transport.
            // I hate when I see stacks doing that...
            final int port = vip != null ? vip.getPort() : connection.getLocalPort();
            if (port == connection.getDefaultPort()) {
                c.withPort(-1);
            } else {
                c.withPort(port);
            }
        });

        ctx().forwardDownstream(event);
    }

    // ============================================
    // === When we use double CRLF as the ping
    // ============================================
    private void onEnterDblCRLFPing(final IOEvent event) {
        // send the double CRLF as a ping
        // start timer no 3
    }

    private void onExitDblCRLFPing(final IOEvent event) {
        // stop timer
        timeoutTimer.ifPresent(t -> t.cancel());
    }

    private void onDblCRLFPing(final IOEvent event) {
        if (event.isPongMessageIOEvent()) {
            become(FlowState.ACTIVE);
        } else if (event.isSipMessageIOEvent()) {
            ctx().forward(event);
            become(FlowState.ACTIVE);
        }
    }

    // ============================================
    // === When we use SIP OPTIONS as ping
    // ============================================
    private void onEnterSipOptionsPing(final IOEvent event) {
        timeoutTimer = Optional.of(ctx().scheduler().schedule(SipTimer.Timeout3, Duration.ofMillis(500)));
        if (config.getKeepAliveConfiguration().getEnforcePong()) {
            // we are essentially re-inventing a basic transaction here
            // and the reason for not using an actual transaction is that
            // first of all, we are "below" the transaction layer so they are not
            // actually available to this layer. Also, we may ignore the 200 OK
            // because we don't enforce the pong, in which case we cannot use a
            // transaction.
            // TODO: need to use the regular T1 values etc.
        }

        final SipURI target = getSipOptionsTarget();
        final FromHeader from = getSipOptionsFrom();
        final ToHeader to = getSipOptionsTo();

        final SipMessage.Builder<SipRequest> builder = SipRequest.options(target)
                .withFromHeader(from)
                .withToHeader(to)
                .withTopMostViaHeader()
                .onTopMostViaHeader(v -> v.withHost(connection.getLocalIpAddress())
                        .withPort(connection.getLocalPort())
                        .withTransport(connection.getTransport().toString().toUpperCase())
                        .withBranch());

        final SipRequest options = builder.build();
        optionsTransaction = TransactionId.create(options);
        ctx().forwardDownstream(IOEvent.create(connection, options));
    }


    private void onExitSipOptionsPing(final IOEvent event) {
        // stop timer
        timeoutTimer.ifPresent(t -> t.cancel());
    }

    private void onSipOptionsPing(final IOEvent event) {
        if (event.isPongMessageIOEvent()) {
            become(FlowState.ACTIVE);
        } else if (event.isSipMessageIOEvent()) {
            if (isOutstandingSipOptionsPong(event.toSipMessageIOEvent())) {
                // reset and remember, we will NOT forward the response
                // up the chain...
                optionsTransaction = null;
                become(FlowState.ACTIVE, "Pong received");
            } else {
                become(FlowState.ACTIVE, "Data received over the flow");
                ctx().forward(event);
            }
        } else if (event.isSipTimerTimeout3()) {
            System.err.println("Man, no pong and nothing else has been received...");
            // TODO: schedule the timer again and back off.
        }

    }

    private boolean isOutstandingSipOptionsPong(final SipMessageIOEvent event) {
        final SipMessage msg = event.toSipMessageIOEvent().message();
        return msg.isResponse() && msg.isOptions() && TransactionId.create(msg).equals(optionsTransaction);
    }

    // =====================
    // === Closing State
    // =====================
    private void onEnterClosing(final IOEvent event) {
        connection.close();
    }

    private void onExitClosing(final IOEvent event) {
    }

    private void onClosing(final IOEvent event) {
        if (event.isConnectionIOEvent()) {
            System.err.println("Connection closed IO event");
            become(FlowState.CLOSED);
        }

        // else, consume...
    }

    // =====================
    // === Closed State, which is the terminal state
    // =====================
    private void onEnterClosed(final IOEvent event) {
        lifeTimeTimer.ifPresent(t -> t.cancel());
    }

    private void onClosed(final IOEvent event) {
        // nothing to do. Terminal state. System should kill us
    }

    /**
     * TODO: we need to pass in the event we got so
     * we can actually send the correct type of pong back.
     * Need to create and actual PingIOEvent class and I guess
     * if we are configured to accept SIP OPTIONS as pings we
     * have to turn that into a ping event and not a regular SIP
     * message. Or should we still allow someone to pass the SIP
     * options up to the application stack eventually???
     */
    private void sendPong() {

    }

    @Override
    protected Logger logger() {
        return logger;
    }

    @Override
    public Flow flow() {
        return new DefaultFlow(connection, state());
    }

    @Override
    public FlowId id() {
        return flowId;
    }

    @Override
    public Connection connection() {
        return connection;
    }

    private SipURI getSipOptionsTarget() {
        final SipOptionsPingConfiguration optionsConfig = keepAliveMethodConfig.getSipOptionsConfiguration();
        return SipURI.withHost(connection.getRemoteIpAddress())
                .withPort(connection.getRemotePort())
                .withUser(optionsConfig.getTargetUser())
                .build();
    }

    private FromHeader getSipOptionsFrom() {
        final SipOptionsPingConfiguration optionsConfig = keepAliveMethodConfig.getSipOptionsConfiguration();
        final String host = optionsConfig.getFromHost().orElseGet(() -> connection.getLocalIpAddress());
        final Address fromAddress = Address.withHost(host)
                .withPort(connection.getLocalPort())
                .withUser(optionsConfig.getFromUser())
                .build();

        return FromHeader.withAddress(fromAddress).build();
    }

    private ToHeader getSipOptionsTo() {
        final SipOptionsPingConfiguration optionsConfig = keepAliveMethodConfig.getSipOptionsConfiguration();
        final String host = optionsConfig.getToHost().orElseGet(() -> connection.getRemoteIpAddress());
        final Address toAddress = Address.withHost(host)
                .withPort(connection.getRemotePort())
                .withUser(optionsConfig.getToUser())
                .build();

        return ToHeader.withAddress(toAddress).build();
    }
}
