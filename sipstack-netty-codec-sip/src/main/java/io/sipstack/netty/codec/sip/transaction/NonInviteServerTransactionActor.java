package io.sipstack.netty.codec.sip.transaction;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.actor.ActorSupport;
import io.sipstack.netty.codec.sip.actor.Cancellable;
import io.sipstack.netty.codec.sip.config.TimersConfiguration;
import io.sipstack.netty.codec.sip.config.TransactionLayerConfiguration;
import io.sipstack.netty.codec.sip.event.Event;
import io.sipstack.netty.codec.sip.event.SipMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Implements the Non-Invite Server Transaction as specified by rfc3261 section
 * 17.2.2.
 *
 * <pre>
 *
 *                                |Request received
 *                                |pass to TU
 *                                V
 *                          +-----------+
 *                          |           |
 *                          | Trying    |-------------+
 *                          |           |             |
 *                          +-----------+             |200-699 from TU
 *                                |                   |send response
 *                                |1xx from TU        |
 *                                |send response      |
 *                                |                   |
 *             Request            V      1xx from TU  |
 *             send response+-----------+send response|
 *                 +--------|           |--------+    |
 *                 |        | Proceeding|        |    |
 *                 +------->|           |<-------+    |
 *          +<--------------|           |             |
 *          |Trnsprt Err    +-----------+             |
 *          |Inform TU            |                   |
 *          |                     |                   |
 *          |                     |200-699 from TU    |
 *          |                     |send response      |
 *          |  Request            V                   |
 *          |  send response+-----------+             |
 *          |      +--------|           |             |
 *          |      |        | Completed |<------------+
 *          |      +------->|           |
 *          +<--------------|           |
 *          |Trnsprt Err    +-----------+
 *          |Inform TU            |
 *          |                     |Timer J fires
 *          |                     |-
 *          |                     |
 *          |                     V
 *          |               +-----------+
 *          |               |           |
 *          +-------------->| Terminated|
 *                          |           |
 *                          +-----------+
 * </pre>
 */
public class NonInviteServerTransactionActor extends ActorSupport<Event, TransactionState> implements TransactionActor {

    private static final Logger logger = LoggerFactory.getLogger(NonInviteServerTransactionActor.class);

    private final TransactionId id;

    private final SipRequest originalRequest;

    private SipMessageEvent lastResponse;

    private final TransactionLayerConfiguration config;

    private final TimersConfiguration timersConfig;

    /**
     * If we enter the completed state, Timer J will let us know when it is time to transition
     * over to the terminated state. The purpose of waiting in the completed state is to absorb
     * any re-transmissions. When timer J fires we will transition over to the terminated
     * state.
     */
    private Cancellable timerJ;

    protected NonInviteServerTransactionActor(final TransactionId id,
                                           final SipRequest request,
                                           final TransactionLayerConfiguration config) {
        super(id.toString(), TransactionState.INIT, TransactionState.TERMINATED, TransactionState.values());

        this.id = id;
        this.originalRequest = request;
        this.config = config;
        this.timersConfig = config.getTimers();

        // setup all the states and the function we are supposed to
        // execute while in that state.
        when(TransactionState.INIT, init);

        when(TransactionState.TRYING, trying);

        when(TransactionState.PROCEEDING, proceeding);

        when(TransactionState.COMPLETED, completed);
        onEnter(TransactionState.COMPLETED, onEnterCompleted);

        // NOTE: no code needed for the terminated state.
        // once we get there we are simply dead and no messages
        // should be processed anyway.
    }

    /**
     * The init state. Just make sure that the first event we receive is the same INVITE as created
     * the transaction (yes, we compare references in this case, that's what we want) and then
     * transition over to the proceeding state.
     */
    private final Consumer<Event> init = event -> {
        if (event.isSipMessageEvent() && event.toSipMessageEvent().message() == originalRequest()) {
            ctx().forwardUpstream(event);
        } else {
            System.err.println("Queue??? shouldn't be able to happen. " + event);
        }

        become(TransactionState.TRYING);
    };

    /**
     * Implements the trying state as follows:
     *
     * <pre>
     *          |Request received
     *          |pass to TU
     *          V
     *    +-----------+
     *    |           |
     *    | Trying    |-------------+
     *    |           |             |
     *    +-----------+             |200-699 from TU
     *          |                   |send response
     *          |1xx from TU        |
     *          |send response      |
     *          |                   |
     *          V                   V
     *    +------------+     +-----------+
     *    | Proceeding |     | Completed |
     *    +------------+     +-----------+
     *
     * </pre>
     */
    private final Consumer<Event> trying = event -> {
        if (event.isSipMessageEvent()) {
            final SipMessageEvent sipMsgEvent = event.toSipMessageEvent();
            final SipMessage msg = sipMsgEvent.message();

            if (msg.isResponse()) {
                final SipResponse response = msg.toResponse();
                relayResponse(sipMsgEvent);
                if (response.isProvisional()) {
                    become(TransactionState.PROCEEDING);
                } else {
                    become(TransactionState.COMPLETED);
                    // become(TransactionState.TERMINATED);
                }
            }
            // note that any request is simply absorbed.
            // unless we actually want to have a retransmited
            // hook as well, which we probably do want.

        } else {
            throw new RuntimeException("Currently not handling the event: " + event);
        }
    };

    /**
     * Implements the proceeding state as follows:
     *
     * <pre>
     *                                |
     *                                |1xx from TU
     *                                |send response
     *                                |
     *             Request            V      1xx from TU
     *             send response+-----------+send response
     *                 +--------|           |--------+
     *                 |        | Proceeding|        |
     *                 +------->|           |<-------+
     *          +<--------------|           |
     *          |Trnsprt Err    +-----------+
     *          |Inform TU            |
     *          |                     |
     *          |                     |200-699 from TU
     *          |                     |send response
     *          |                     V
     *    +-------------+       +-----------+
     *    | Terminated  |       | Completed |
     *    +-------------+       +-----------+
     *
     * </pre>
     */
    private final Consumer<Event> proceeding = event -> {
        if (event.isSipMessageEvent()) {
            final SipMessageEvent sipMsgEvent = event.toSipMessageEvent();
            final SipMessage msg = sipMsgEvent.message();

            // any request is treated as a retransmission
            // at this point since is must belong to the same
            // transaction if it ends up here. However, perhaps
            // we should actually check that it is the same method
            // etc...
            if (msg.isRequest()) {
                relayResponse(lastResponse);
                return;
            }

            final SipResponse response = msg.toResponse();
            relayResponse(sipMsgEvent);
            if (response.isFinal()) {
                become(TransactionState.COMPLETED);
            }
        } else {
            throw new RuntimeException("Currently not handling the event: " + event);
        }
    };

    /**
     * Implements the completed state, which is:
     *
     * <pre>
     *                                |
     *                                |200-699 from TU
     *                                |send response
     *             Request            V
     *             send response+-----------+
     *                 +--------|           |
     *                 |        | Completed |
     *                 +------->|           |
     *          +<--------------|           |
     *          |Trnsprt Err    +-----------+
     *          |Inform TU            |
     *          |                     |Timer J fires
     *          |                     |-
     *          |                     |
     *          |                     V
     *          |               +-----------+
     *          |               |           |
     *          +-------------->| Terminated|
     *                          |           |
     *                          +-----------+
     *
     * </pre>
     */
    private final Consumer<Event> completed = event -> {
        if (event.isSipMessageEvent()) {
            final SipMessageEvent sipMsgEvent = event.toSipMessageEvent();
            final SipMessage msg = sipMsgEvent.message();

            // We are guaranteed that any request being passed to
            // this transaction is actually for this transaction only
            // and therefore it must be a re-transmitted request
            // so no need to check any further.
            if (msg.isRequest()) {
                relayResponse(lastResponse);
                return;
            }

        } else if (event.isSipTimerJ()) {
            become(TransactionState.TERMINATED);
        }
    };

    /**
     * All we have to do when entering the completed state is to setup timer J.
     * Note, we do not have an on exit for completed since the only way you
     * can get out of the completed state is when timer J fires.
     *
     * Note, the way this is implemented is that we will schedule timer J to fire in zero
     * seconds for reliable transports so we don't have to keep track of whether or not
     * we scheduled one.
     */
    private final Consumer<Event> onEnterCompleted = event -> {
        // TODO: check if the initial message was sent over a reliable transport.
        // TODO: add a isReliableTransport to the SipMessage
        final Duration duration = timerConfig().getTimerJ();
        timerJ = scheduleTimer(SipTimer.J, duration);
    };

    private void relayResponse(final SipMessageEvent event) {
        if (lastResponse == null
                || lastResponse.message().toResponse().getStatus() < event.message().toResponse().getStatus()) {
            lastResponse = event;
        }
        ctx().forwardDownstream(event);
    }

    private final Cancellable scheduleTimer(final SipTimer timer, final Duration duration) {
        return ctx().scheduler().schedule(timer, duration);
    }

    private SipMessage originalRequest() {
        return originalRequest;
    }

    private TimersConfiguration timerConfig() {
        return timersConfig;
    }

    @Override
    protected Logger logger() {
        return logger;
    }

    @Override
    public TransactionId id() {
        return id;
    }


    public Transaction transaction() {
        return new DefaultTransaction(id, currentState);
    }

}
