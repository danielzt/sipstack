package io.sipstack.transaction.impl;

import io.hektor.core.ActorRef;
import io.hektor.core.Cancellable;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.ActorSupport;
import io.sipstack.event.Event;
import io.sipstack.event.IOEvent;
import io.sipstack.event.IOReadEvent;
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
public class NonInviteServerTransactionActor extends ActorSupport<Event, TransactionState> {

    private static final Logger logger = LoggerFactory.getLogger(NonInviteServerTransactionActor.class);

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

    private final IOReadEvent<SipMessage> originalRequest;

    private final TransactionLayerConfiguration config;

    private IOEvent<SipMessage> lastResponse;

    /**
     * If we enter the completed state we will setup Timer J for response retransmissions if this
     * transaction is over an unreliable transport such as UDP.
     */
    private Cancellable timerJ = null;


    public NonInviteServerTransactionActor(final ActorRef upstream,
                                           final TransactionId id,
                                           final IOReadEvent<SipMessage> request,
                                           final TransactionLayerConfiguration config) {
        super(id.toString(), TransactionState.INIT, TransactionState.values());

        this.upstream = upstream;
        this.id = id;
        this.originalRequest = request;
        this.config = config;

        // setup all the states and the function we are supposed to
        // execute while in that state.
        when(TransactionState.INIT, init);

        when(TransactionState.TRYING, trying);

        when(TransactionState.PROCEEDING, proceeding);

        when(TransactionState.COMPLETED, completed);
        onEnter(TransactionState.COMPLETED, onEnterCompleted);

        // no exit from terminated. You are dead
        when(TransactionState.TERMINATED, terminated);
        onEnter(TransactionState.TERMINATED, onEnterTerminated);
    }

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
        if (event.isSipIOEvent()) {
            final IOEvent<SipMessage> sipEvent = event.toSipIOEvent();
            final SipMessage msg = sipEvent.getObject();
            if (msg.isResponse()) {
                final SipResponse response = msg.toResponse();
                relayResponse(sipEvent);
                if (response.isProvisional()) {
                    become(TransactionState.PROCEEDING);
                } else {
                    become(TransactionState.COMPLETED);
                    // become(TransactionState.TERMINATED);
                }
            }
            // note that any request is simply absorbed.

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
        if (event.isSipIOEvent()) {
            final IOEvent<SipMessage> sipEvent = event.toSipIOEvent();
            final SipMessage msg = sipEvent.getObject();

            if (msg.isRequest()) {
                sender().tell(lastResponse, self());
                return;
            }

            final SipResponse response = msg.toResponse();
            relayResponse(sipEvent);
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
        if (event.isSipIOEvent()) {
            final IOEvent<SipMessage> sipEvent = event.toSipIOEvent();
            final SipMessage msg = sipEvent.getObject();

            // We are guaranteed that any request being passed to
            // this transaction is actually for this transaction only
            // and therefore it must be a re-transmitted request
            // so no need to check any further.
            if (msg.isRequest()) {
                sender().tell(lastResponse, self());
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
        final SipTimerEvent timerEvent = SipTimerEvent.withTimer(SipTimer.J).build();
        timerJ = ctx().scheduler().schedule(timerEvent, self(), self(), duration);
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

    private void relayResponse(final IOEvent<SipMessage> response) {
        if (lastResponse == null
                || lastResponse.getObject().toResponse().getStatus() < response.getObject().toResponse().getStatus()) {
            lastResponse = response;
        }
        downstream.tell(response, sender());
    }

    /**
     * The init state. Just make sure that the first event we receive is the same INVITE as created
     * the transaction (yes, we compare references in this case, that's what we want) and then
     * transition over to the proceeding state.
     */
    private final Consumer<Event> init = event -> {
        if (event == initialRequest()) {
            downstream = sender();
            upstream().tell(event, self());
        } else {
            System.err.println("Queue??? shouldn't be able to happen");
        }
        become(TransactionState.TRYING);
    };

    private TimersConfiguration timerConfig() {
        return config.getTimers();
    }

    private TransactionLayerConfiguration config() {
        return config;
    }

    private ActorRef upstream() {
        return upstream;
    }

    private IOReadEvent<SipMessage> initialRequest() {
        return originalRequest;
    }


    @Override
    protected Logger logger() {
        return logger;
    }
}
