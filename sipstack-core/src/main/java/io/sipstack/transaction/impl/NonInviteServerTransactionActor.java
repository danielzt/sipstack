/**
 * 
 */
package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.*;
import io.sipstack.config.TimersConfiguration;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.event.Event;
import io.sipstack.event.SipMsgEvent;
import io.sipstack.timers.SipTimer;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.BiConsumer;


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
 public class NonInviteServerTransactionActor extends ActorBase<TransactionState> implements TransactionActor {

    private static final Logger logger = LoggerFactory.getLogger(NonInviteServerTransactionActor.class);

    private final TransactionId id;

    /**
     * The initial request that created this transaction.
     */
    private final SipMsgEvent request;

    private final TransactionSupervisor parent;

    /**
     * The timer configuration contains all the timers used by SIP so that we know
     * what value e.g. Timer J should have etc.
     */
    private final TimersConfiguration timers;

    /**
     * The configuration regarding how the transaction layer (which this actor is part of)
     * should behave.
     */
    private final TransactionLayerConfiguration transactionConfig;

    /**
     * If we receive any re-transmissions, we will send out the last response again if there is
     * one..
     */
    private SipMsgEvent lastResponseEvent;

    /**
     * If we enter the completed state we will setup Timer J for response retransmissions if this
     * transaction is over an unreliable transport such as UDP.
     */
    private Scheduler.Cancellable timerJ = null;

    /**
     *
     */
    protected NonInviteServerTransactionActor(final TransactionSupervisor parent, final TransactionId id,
                                              final SipMsgEvent requestEvent) {
        super(id.toString(), TransactionState.INIT, TransactionState.values());
        this.timers = parent.getConfig().getTimers();
        this.transactionConfig = parent.getConfig();
        this.parent = parent;
        this.id = id;
        this.request = requestEvent;

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

    private SipMsgEvent getInitialRequestEvent() {
        return request;
    }

    private TimersConfiguration getTimerConfig() {
        return timers;
    }

    private TransactionLayerConfiguration getConfig() {
        return transactionConfig;
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
    private final BiConsumer<ActorContext, Event> trying = (ctx, event) -> {
        if (event.isSipMsgEvent()) {
            final SipMsgEvent sipEvent = (SipMsgEvent) event;
            final SipMessage msg = sipEvent.getSipMessage();
            if (msg.isResponse()) {
                final SipResponse response = msg.toResponse();
                relayResponse(ctx, sipEvent);
                if (response.isProvisional()) {
                    become(TransactionState.PROCEEDING);
                } else {
                    become(TransactionState.COMPLETED);
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
    private final BiConsumer<ActorContext, Event> proceeding = (ctx, event) -> {
            if (event.isSipMsgEvent()) {
                final SipMsgEvent sipEvent = (SipMsgEvent) event;
                final SipMessage msg = sipEvent.getSipMessage();
                if (msg.isRequest()) {
                    ctx.reverse().forward(lastResponseEvent);
                    return;
                }

                final SipResponse response = msg.toResponse();
                relayResponse(ctx, sipEvent);
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
    private final BiConsumer<ActorContext, Event> completed = (ctx, event) -> {
        if (event.isSipMsgEvent()) {
            SipMsgEvent sipEvent = event.toSipMsgEvent();
            SipMessage msg = sipEvent.getSipMessage();

            // We are guaranteed that any request being passed to
            // this transaction is actually for this transaction only
            // and therefore it must be a re-transmitted request
            // so no need to check any further.
            if (msg.isRequest()) {
                ctx.reverse().forward(lastResponseEvent);
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
    private final BiConsumer<ActorContext, Event> onEnterCompleted = (ctx, event) -> {
        // TODO: check if the initial message was sent over a reliable transport.
        // TODO: add a isReliableTransport to the SipMessage
        final Duration duration = getTimerConfig().getTimerJ();
        timerJ = ctx.scheduler().schedule(duration, SipTimer.J);
    };


    /**
     * Implements the state terminated which really doesn't do anything at all.
     */
    private final BiConsumer<ActorContext, Event> terminated = (ctx, event) -> {
        // left empty intentionally
    };

    /**
     * Implements the state terminated which really doesn't do much other then dies...
     */
    private final BiConsumer<ActorContext, Event> onEnterTerminated = (ctx, event) -> {
        ctx.killMe();;
    };

    private void relayResponse(final ActorContext ctx, final SipMsgEvent event) {
        if (lastResponseEvent == null
                || lastResponseEvent.getSipMessage().toResponse().getStatus() < event.getSipMessage().toResponse()
                .getStatus()) {
            lastResponseEvent = event;
        }
        ctx.forward(event);
    }

    /**
     * The init state. Just make sure that the first event we receive is the same INVITE as created
     * the transaction (yes, we compare references in this case, that's what we want) and then
     * transition over to the proceeding state.
     */
    private final BiConsumer<ActorContext, Event> init = (ctx, event) -> {
        if (event == getInitialRequestEvent()) {
            ctx.forward(event);
        } else {
            System.err.println("Queue??? shouldn't be able to happen");
        }
        become(TransactionState.TRYING);
    };

    @Override
    public Transaction getTransaction() {
        return new ServerTransactionImpl(this.id, state());
    }

    @Override
    public Supervisor getSupervisor() {
        return this.parent;
    }

    @Override
    public TransactionId getTransactionId() {
        return this.id;
    }

    @Override
    protected final Logger logger() {
        return logger;
    }

}
