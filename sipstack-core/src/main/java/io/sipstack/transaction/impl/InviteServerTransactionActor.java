package io.sipstack.transaction.impl;

import io.hektor.core.ActorRef;
import io.hektor.core.Cancellable;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.ActorSupport;
import io.sipstack.core.Utils;
import io.sipstack.event.Event;
import io.sipstack.event.IOReadEvent;
import io.sipstack.event.IOWriteEvent;
import io.sipstack.event.SipTimerEvent;
import io.sipstack.netty.codec.sip.config.TimersConfiguration;
import io.sipstack.netty.codec.sip.config.TransactionLayerConfiguration;
import io.sipstack.timers.SipTimer;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Actually, need to implement the updated state machine as specified by rfc6026
 *
 * <pre>
 *
 *                                    |INVITE
 *                                    |pass INV to TU
 *                 INVITE             V send 100 if TU won't in 200 ms
 *                 send response+------------+
 *                     +--------|            |--------+ 101-199 from TU
 *                     |        |            |        | send response
 *                     +------->|            |<-------+
 *                              | Proceeding |
 *                              |            |--------+ Transport Err.
 *                              |            |        | Inform TU
 *                              |            |<-------+
 *                              +------------+
 *                 300-699 from TU |    |2xx from TU
 *                 send response   |    |send response
 *                  +--------------+    +------------+
 *                  |                                |
 * INVITE           V          Timer G fires         |
 * send response +-----------+ send response         |
 *      +--------|           |--------+              |
 *      |        |           |        |              |
 *      +------->| Completed |<-------+      INVITE  |  Transport Err.
 *               |           |               -       |  Inform TU
 *      +--------|           |----+          +-----+ |  +---+
 *      |        +-----------+    | ACK      |     | v  |   v
 *      |          ^   |          | -        |  +------------+
 *      |          |   |          |          |  |            |---+ ACK
 *      +----------+   |          |          +->|  Accepted  |   | to TU
 *      Transport Err. |          |             |            |<--+
 *      Inform TU      |          V             +------------+
 *                     |      +-----------+        |  ^     |
 *                     |      |           |        |  |     |
 *                     |      | Confirmed |        |  +-----+
 *                     |      |           |        |  2xx from TU
 *       Timer H fires |      +-----------+        |  send response
 *       -             |          |                |
 *                     |          | Timer I fires  |
 *                     |          | -              | Timer L fires
 *                     |          V                | -
 *                     |        +------------+     |
 *                     |        |            |<----+
 *                     +------->| Terminated |
 *                              |            |
 *                              +------------+
 *
 * </pre>
 */
public class InviteServerTransactionActor extends ActorSupport<Event, TransactionState> {

    private static final Logger logger = LoggerFactory.getLogger(InviteServerTransactionActor.class);

    /**
     *
     */
    private final ActorRef upstream;

    /**
     * This will typically be a FlowActor but if that actor fails
     * we may have to reset it to a TransportSupervisor.
     */
    private ActorRef downstream;

    private final TransactionId id;
    private final IOReadEvent<SipMessage> originalInvite;

    private final TransactionLayerConfiguration config;

    private final TimersConfiguration timersConfig;

    private IOWriteEvent<SipMessage> lastResponse;

    private Cancellable timer100Trying;

    /**
     * If we enter the completed state we will setup Timer G for response retransmissions if this
     * transaction is over an unreliable transport.
     */
    private Cancellable timerG = null;


    /**
     * How many times Timer G has fired.
     */
    private int timerGCount = 0;

    /**
     * If we enter the completed state, Timer H will take us from completed to terminated unless we
     * do receive an ACK before then. Hence, Timer H determines the longest amount of time we can
     * stay in the completed state.
     */
    private Cancellable timerH = null;

    /**
     * If we enter the accepted state, Timer L will let us know when it is time to transition
     * over to the terminated state. The accepted state is a "new" state introduced by RFC6026
     * in order to deal with re-transmitted INVITEs. In the old state machine, we transitioned
     * directly to terminated state as soon as we got a 2xx to the INVITE which meant that
     * any re-transmissions on the INVITE created a brand new transaction, which is not what
     * we want.
     */
    private Cancellable timerL = null;

    public InviteServerTransactionActor(final ActorRef upstream,
                                        final TransactionId id,
                                        final IOReadEvent<SipMessage> invite,
                                        final TransactionLayerConfiguration config) {
        super(id.toString(), TransactionState.INIT, TransactionState.values());
        this.upstream = upstream;
        this.id = id;
        this.originalInvite = invite;
        this.config = config;
        this.timersConfig = config.getTimers();

        when(TransactionState.INIT, init);

        when(TransactionState.PROCEEDING, proceeding);
        onEnter(TransactionState.PROCEEDING, onEnterProceeding);
        onExit(TransactionState.PROCEEDING, onExitProceeding);

        when(TransactionState.ACCEPTED, accepted);
        onEnter(TransactionState.ACCEPTED, onEnterAccepted);
        onExit(TransactionState.ACCEPTED, onExitAccepted);

        when(TransactionState.COMPLETED, completed);
        onEnter(TransactionState.COMPLETED, onEnterCompleted);
        onExit(TransactionState.COMPLETED, onExitCompleted);

        when(TransactionState.CONFIRMED, confirmed);
        onEnter(TransactionState.CONFIRMED, onEnterConfirmed);

        // no exit from terminated. You are dead
        when(TransactionState.TERMINATED, terminated);
        onEnter(TransactionState.TERMINATED, onEnterTerminated);

    }

    /**
     * The init state. Just make sure that the first event we receive is the same INVITE as created
     * the transaction (yes, we compare references in this case, that's what we want) and then
     * transition over to the proceeding state.
     */
    private final Consumer<Event> init = event -> {
        if (event == getInitialInviteEvent()) {
            downstream = sender();
            upstream().tell(event, self());
        } else {
            System.err.println("Queue??? shouldn't be able to happen. " + event);
        }
        become(TransactionState.PROCEEDING);
    };

    /**
     * Implements the proceeding state as follows:
     *
     * <pre>
     *
     *                                    |INVITE
     *                                    |pass INV to TU
     *                 INVITE             V send 100 if TU won't in 200 ms
     *                 send response+------------+
     *                     +--------|            |--------+ 101-199 from TU
     *                     |        |            |        | send response
     *                     +------->|            |<-------+
     *                              | Proceeding |
     *                              |            |--------+ Transport Err.
     *                              |            |        | Inform TU
     *                              |            |<-------+
     *                              +------------+
     *                 300-699 from TU |    |2xx from TU
     *                 send response   |    |send response
     *                  +--------------+    +------------+
     *                  |                                |
     *                  V                                V
     *         +------------+                     +------------+
     *         |  Completed |                     |  Accepted  |
     *         +------------+                     +------------+
     * </pre>
     */
    private final Consumer<Event> proceeding = event -> {
        if (event.isSipIOEvent()) {
            final SipMessage msg = event.toSipIOEvent().getObject();
            if (isRetransmittedInvite(msg)) {
                if (lastResponse != null) {
                    relayResponse(lastResponse);
                }
                return;
            }

            final SipResponse response = msg.toResponse();
            relayResponse(event.toSipIOWriteEvent());
            if (response.isSuccess()) {
                become(TransactionState.ACCEPTED);
                // become(TransactionState.TERMINATED);
            } else if (response.isFinal()) {
                become(TransactionState.COMPLETED);
            }
        } else if (event.isSipTimer100Trying()) {
            final SipResponse response = getInitialInviteEvent().getObject().createResponse(100);
            downstream.tell(IOWriteEvent.create(response), self());
        } else {
            unhandled(event);
        }
    };

    /**
     * When entering the Proceeding state we may send a 100 Trying right away (depending on configuration) or we may
     * delay it with 200 ms and send it later unless TU already have sent some kind of response (any kind really)
     */
    private final Consumer<Event> onEnterProceeding = event -> {
        if (config().isSend100TryingImmediately()) {
            final SipMessage invite = getInitialInviteEvent().getObject();
            if (invite.isAck()) {
                System.err.println("What the fuck, this is an ACK!");
            }
            final SipResponse response = invite.createResponse(100);
            downstream.tell(IOWriteEvent.create(response), self());
        } else {
            timer100Trying = ctx().scheduler().schedule(SipTimer.Trying, self(), self(), Duration.ofMillis(200));
        }
    };

    private final Consumer<Event> onExitProceeding = event -> {
        if (timer100Trying != null) {
            timer100Trying.cancel();
        }
    };

    /**
     * Implements the accepted state as follows:
     *
     * <pre>
     *
     *
     *    INVITE     Transport Err.
     *    -          Inform TU
     *    +-----+    +---+
     *    |     |    |   v
     *    |  +------------+
     *    |  |            |---+ ACK
     *    +->|  Accepted  |   | to TU
     *       |            |<--+
     *       +------------+
     *          |  ^     |
     *          |  |     |
     *          |  +-----+
     *          |  2xx from TU
     *          |  send response
     *          |
     *          |
     *          | Timer L fires
     *          | -
     *          V
     *   +------------+
     *   | Terminated |
     *   +------------+
     *
     * </pre>
     */
    private final Consumer<Event> accepted = event -> {
        if (event.isSipIOEvent()) {
            final SipMessage msg = event.toSipIOEvent().getObject();
            if (msg.isResponse() && msg.toResponse().isSuccess()) {
                // only 2xx responses are forwarded. The rest are consumed.
                // see above state machine
                relayResponse(event.toSipIOWriteEvent());
            } else if (isRetransmittedInvite(msg)) {
                // absorb. According to rfc6026, don't
                // event forward it to TU...
            }
        } else if (event.isSipTimerL()) {
            become(TransactionState.TERMINATED);
        }
    };

    /**
     *
     */
    private final Consumer<Event> onEnterAccepted = event -> {
        final Duration duration = timersConfig().getTimerL();
        timerL = scheduleTimer(SipTimer.L, duration);
    };

    private final Consumer<Event> onExitAccepted = event -> {
        timerL.cancel();
    };


    /**
     * The primary purpose of the confirmed state is to absorb any re-transmitted ACKs
     * so therefore we will simply absorb everything in this state and really only
     * react to Timer I, which will take us to the terminated state.
     */
    private final Consumer<Event> confirmed = event -> {
        if (event.isSipTimerI()) {
            become(TransactionState.TERMINATED);
        }

        // anything else is absorbed.
    };

    private final Consumer<Event> onEnterConfirmed = event -> {
        scheduleTimer(SipTimer.I, timersConfig().getTimerI());
    };

    /**
     * Implements the completed state, which is:
     *
     * <pre>
     *
     * INVITE                      Timer G fires
     * send response +-----------+ send response         |
     *      +--------|           |--------+
     *      |        |           |        |
     *      +------->| Completed |<-------+
     *               |           |
     *      +--------|           |----+
     *      |        +-----------+    | ACK
     *      |          ^   |          | -
     *      |          |   |          |
     *      +----------+   |          |
     *      Transport Err. |          |
     *      Inform TU      |          V
     *                     |      +-----------+
     *                     |      |           |
     *                     |      | Confirmed |
     *                     |      |           |
     *       Timer H fires |      +-----------+
     *       -             v
     *              +------------+
     *              |            |
     *              | Terminated |
     *              |            |
     *              +------------+
     * </pre>
     */
    private final Consumer<Event> completed = event -> {
        if (event.isSipIOEvent()) {
            final SipMessage msg = event.toSipIOEvent().getObject();

            if (msg.isAck()) {
                become(TransactionState.CONFIRMED);
            } else if (isRetransmittedInvite(msg)) {
                if (lastResponse != null) {
                    relayResponse(lastResponse);
                }
            }
        } else if (event.isSipTimerG()) {
            ++timerGCount;
            timerG = scheduleTimer(SipTimer.G, calculateNextTimerG());
            relayResponse(lastResponse);
        } else if (event.isSipTimerH()) {
            become(TransactionState.TERMINATED);
        }
    };

    private final Consumer<Event> onEnterCompleted = event -> {
        timerG = scheduleTimer(SipTimer.G, calculateNextTimerG());
        timerH = scheduleTimer(SipTimer.H, timersConfig().getTimerH());
    };

    private final Consumer<Event> onExitCompleted = event -> {
        timerG.cancel();
        timerH.cancel();
    };


    /**
     * Implements the state terminated which really doesn't do anything at all.
     */
    private final Consumer<Event> terminated = event -> {
        // left empty intentionally
    };

    /**
     * Implements the state terminated which really doesn't do much other then dies...
     */
    private final Consumer<Event> onEnterTerminated = event -> {
        ctx().stop();
    };

    private final Cancellable scheduleTimer(final SipTimer timer, final Duration duration) {
        return ctx().scheduler().schedule(SipTimerEvent.withTimer(timer).build(), self(), self(), duration);
    }

    private TransactionLayerConfiguration config() {
        return config;
    }

    private TimersConfiguration timersConfig() {
        return timersConfig;
    }

    /**
     * While in the completed state we will re-transmit the final response (which then must be a
     * error response only otherwise we wouldn't be in the completed state to begin with) every time
     * this timer fires.
     *
     * @return
     */
    private Duration calculateNextTimerG() {
        final long defaultTimerG = timersConfig().getTimerG().toMillis();
        final long t2 = timersConfig().getT2().toMillis();
        return Utils.calculateBackoffTimer(timerGCount, defaultTimerG, t2);
    }

    private void relayResponse(final IOWriteEvent<SipMessage> response) {
        if (lastResponse == null
                || lastResponse.getObject().toResponse().getStatus() < response.getObject().toResponse().getStatus()) {
            lastResponse = response;
        }
        downstream.tell(response, sender());
    }

    private ActorRef upstream() {
        return upstream;
    }

    private IOReadEvent<SipMessage> getInitialInviteEvent() {
        return originalInvite;
    }

    /**
     * Check whether the request is a re-transmitted INVITE.
     * @param msg
     * @return
     */
    private boolean isRetransmittedInvite(SipMessage msg) {
        if (msg.isRequest() && msg.isInvite()) {
            // TODO: actually check it...
            return true;
        }
        return false;
    }

    @Override
    protected final Logger logger() {
        return logger;
    }

}

