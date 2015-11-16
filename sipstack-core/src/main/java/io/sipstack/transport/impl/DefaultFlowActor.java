package io.sipstack.transport.impl;

import io.sipstack.actor.ActorSupport;
import io.sipstack.actor.Cancellable;
import io.sipstack.config.FlowConfiguration;
import io.sipstack.config.KeepAliveConfiguration;
import io.sipstack.event.Event;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.event.ConnectionIOEvent;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowId;
import io.sipstack.transport.FlowState;
import io.sipstack.transport.event.FlowEvent;
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

    private final Connection connection;

    private final FlowId flowId;

    /**
     * There are several cases where we use a timer to either kill the connection
     * because of a lack of activity or for keeping track of a pong etc. In either
     * case, we really only need one at any given time.
     */
    private Optional<Cancellable> timeoutTimer = Optional.empty();

    protected DefaultFlowActor(final FlowConfiguration config, final Connection connection) {
        super(connection.id().toString(), FlowState.INIT, FlowState.CLOSED, FlowState.values());
        this.config = config;
        this.connection = connection;
        this.flowId = FlowId.create(connection.id());

        when(FlowState.INIT, this::onInit);

        onEnter(FlowState.READY, this::onEnterReady);
        when(FlowState.READY, this::onReady);
        onExit(FlowState.READY, this::onExitReady);

        onEnter(FlowState.ACTIVE, this::onEnterActive);
        when(FlowState.ACTIVE, this::onActive);
        onExit(FlowState.ACTIVE, this::onExitActive);

        onEnter(FlowState.WAIT_PONG, this::onEnterWaitPong);
        when(FlowState.WAIT_PONG, this::onWaitPong);
        onExit(FlowState.WAIT_PONG, this::onExitWaitPong);

        onEnter(FlowState.CLOSING, this::onEnterClosing);
        when(FlowState.CLOSING, this::onClosing);
        onExit(FlowState.CLOSING, this::onExitClosing);

        when(FlowState.CLOSED, this::onClosed);
    }

    // =====================
    // === Init State
    // =====================
    private void onInit(final IOEvent event) {
        if (event.isSipMessageIOEvent()) {
            ctx().forward(event);
            become(FlowState.ACTIVE);
        } else if (event.isConnectionIOEvent()) {
            final ConnectionIOEvent connectionEvent = event.toConnectionIOEvent();
            if (connectionEvent.isConnectionOpenedIOEvent()) {
                become(FlowState.READY);
            }
        } else {
            unhandled(event);
        }
    }

    // =====================
    // === Ready State
    // =====================
    private void onEnterReady(final IOEvent event) {
        timeoutTimer = Optional.of(ctx().scheduler().schedule(SipTimer.Timeout, config.getInitialIdleTimeout()));
    }

    private void onExitReady(final IOEvent event) {
        timeoutTimer.ifPresent(t -> t.cancel());
    }

    private void onReady(final IOEvent event) {
        if (event.isSipMessageIOEvent()) {
            ctx().forward(event);
            become(FlowState.ACTIVE);
        } else if (event.isSipTimerTimeout()) {
            become(FlowState.CLOSING);
        } else if (event.isConnectionInactiveIOEvent()) {
            become(FlowState.CLOSING);
        } else if (event.isPingMessageIOEvent()) {
            // TODO: send pong
            become(FlowState.ACTIVE);
        } else if (event.isPongMessageIOEvent()) {
            // just consume
            become(FlowState.ACTIVE);
        }
    }

    // =====================
    // === Active State
    // =====================
    private void onEnterActive(final IOEvent event) {

        // In active mode we will schedule a timer to ensure we get
        // keep-alive traffic and if not, we will issue keep-alive
        // traffic ourselves.
        final KeepAliveConfiguration config = this.config.getKeepAliveConfiguration();
        if (config.getMode() == KeepAliveConfiguration.KEEP_ALIVE_MODE.ACTIVE) {
            timeoutTimer = Optional.of(ctx().scheduler().schedule(SipTimer.Timeout, config.getIdleTimeout()));
        }
    }

    private void onExitActive(final IOEvent event) {
        timeoutTimer.ifPresent(t -> t.cancel());
    }

    private void onActive(final IOEvent event) {
        if (event.isSipMessageIOEvent()) {
            ctx().forward(event);
        } else if (event.isPingMessageIOEvent()) {
            become(FlowState.WAIT_PONG);
        }

        // TODO: need some close event
    }

    // =====================
    // === Wait Pong State
    // =====================
    private void onEnterWaitPong(final IOEvent event) {
        // send ping
        // start timer
    }

    private void onExitWaitPong(final IOEvent event) {
        // stop timer
        timeoutTimer.ifPresent(t -> t.cancel());
    }

    private void onWaitPong(final IOEvent event) {
        if (event.isPongMessageIOEvent()) {
            become(FlowState.ACTIVE);
        } else if (event.isSipMessageIOEvent()) {
            ctx().forward(event);
            become(FlowState.ACTIVE);
        }
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
    private void onClosed(final IOEvent event) {
        // nothing to do. Terminal state. System should kill us
    }

    @Override
    protected Logger logger() {
        return logger;
    }

    @Override
    public Flow flow() {
        return new DefaultFlow(connection);
    }

    @Override
    public FlowId id() {
        return flowId;
    }
}
