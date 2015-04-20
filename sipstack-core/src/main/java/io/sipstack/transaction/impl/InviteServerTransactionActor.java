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
 * Implements the Invite Server Transaction as specified by rfc3261 section
 * 
 * <pre>
 *                     |INVITE
 *                     |pass INV to TU
 *  INVITE             V send 100 if TU won't in 200ms
 *  send response+-----------+
 *      +--------|           |--------+101-199 from TU
 *      |        | Proceeding|        |send response
 *      +------->|           |<-------+
 *               |           |          Transport Err.
 *               |           |          Inform TU
 *               |           |--------------->+
 *               +-----------+                |
 *  300-699 from TU |     |2xx from TU        |
 *  send response   |     |send response      |
 *                  |     +------------------>+
 *                  |                         |
 *  INVITE          V          Timer G fires  |
 *  send response+-----------+ send response  |
 *      +--------|           |--------+       |
 *      |        | Completed |        |       |
 *      +------->|           |<-------+       |
 *               +-----------+                |
 *                  |     |                   |
 *              ACK |     |                   |
 *              -   |     +------------------>+
 *                  |        Timer H fires    |
 *                  V        or Transport Err.|
 *               +-----------+  Inform TU     |
 *               |           |                |
 *               | Confirmed |                |
 *               |           |                |
 *               +-----------+                |
 *                     |                      |
 *                     |Timer I fires         |
 *                     |-                     |
 *                     |                      |
 *                     V                      |
 *               +-----------+                |
 *               |           |                |
 *               | Terminated|<---------------+
 *               |           |
 *               +-----------+
 * 
 * </pre>
 * 
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
public class InviteServerTransactionActor extends ActorBase<TransactionState> implements TransactionActor {

    private static final Logger logger = LoggerFactory.getLogger(InviteServerTransactionActor.class);

    private final TransactionId id;

    private final SipMsgEvent invite;

    private final TransactionSupervisor parent;

    private final TimersConfiguration timers;

    private final TransactionLayerConfiguration transactionConfig;

    /**
     * If we receive any re-transmissions, we will send out the last response again if there is
     * one..
     */
    private SipMsgEvent lastResponseEvent;

    /**
     * If the TU doesn't send 100 Trying within 200ms we will.
     * This timer keeps track of that.
     */
    private Scheduler.Cancellable timer100Trying = null;

    /**
     * Contains all the states and their corresponding function pointers. Makes it for easy
     * transition to new behavior as the state changes.
     */

    /**
     * If we enter the completed state we will setup Timer G for response retransmissions if this
     * transaction is over an unreliable transport.
     */
    private Scheduler.Cancellable timerG = null;


    /**
     * How many times Timer G has fired.
     */
    private int timerGCount = 0;

    /**
     * If we enter the completed state, Timer H will take us from completed to terminated unless we
     * do receive an ACK before then. Hence, Timer H determines the longest amount of time we can
     * stay in the completed state.
     */
    private Scheduler.Cancellable timerH = null;

    /**
     * If we enter the accpted state, Timer L will let us know when it is time to transition
     * over to the terminated state. The accepted state is a "new" state introduced by RFC6026
     * in order to deal with re-transmitted INVITEs. In the old state machine, we transitioned
     * directly to terminated state as soon as we got a 2xx to the INVITE which meant that
     * any re-transmissions on the INVITE created a brand new transaction, which is not what
     * we want.
     */
    private Scheduler.Cancellable timerL = null;

    /**
     * 
     */
    protected InviteServerTransactionActor(final TransactionSupervisor parent, final TransactionId id,
            final SipMsgEvent inviteEvent) {
        super(id.toString(), TransactionState.INIT, TransactionState.values());
        this.timers = parent.getConfig().getTimers();
        this.transactionConfig = parent.getConfig();
        this.parent = parent;
        this.id = id;
        this.invite = inviteEvent;

        // setup all the states and the function we are supposed to
        // execute while in that state.
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

    private SipMsgEvent getInitialInviteEvent() {
        return invite;
    }

    private TimersConfiguration getTimerConfig() {
        return timers;
    }

    private TransactionLayerConfiguration getConfig() {
        return transactionConfig;
    }

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
    private final BiConsumer<ActorContext, Event> proceeding = (ctx, event) -> {
            if (event.isSipMsgEvent()) {
                final SipMsgEvent sipEvent = (SipMsgEvent) event;
                final SipMessage msg = sipEvent.getSipMessage();
                if (isRetransmittedInvite(msg)) {
                    ctx.reverse().forward(lastResponseEvent);
                    return;
                }

                final SipResponse response = msg.toResponse();
                relayResponse(ctx, sipEvent);
                if (response.isSuccess()) {
                    become(TransactionState.ACCEPTED);
                    // become(TransactionState.TERMINATED);
                } else if (response.isFinal()) {
                    become(TransactionState.COMPLETED);
                }
            } else if (event.isSipTimer100Trying()) {
                // Note, we have to reverse the context because when we
                // scheduled the 100 Trying event we had an 'upstream' context
                // since we were processing an incoming INVITE at that time.

                final SipResponse response = getInitialInviteEvent().getSipMessage().createResponse(100);
                ctx.reverse().forward(SipMsgEvent.create(getInitialInviteEvent().key(), response));

            } else {

            }
        };

    /**
     * When entering the Proceeding state we may send a 100 Trying right away (depending on configuration) or we may
     * delay it with 200 ms and send it later unless TU already have sent some kind of response (any kind really)
     */
    private final BiConsumer<ActorContext, Event> onEnterProceeding = (ctx, event) -> {
        if (getConfig().isSend100TryingImmediately()) {
            final SipResponse response = getInitialInviteEvent().getSipMessage().createResponse(100);
            ctx.reverse().forward(SipMsgEvent.create(getInitialInviteEvent().key(), response));
        } else {
            timer100Trying = ctx.scheduler().schedule(Duration.ofMillis(200), SipTimer.Trying);
        }
    };

    private final BiConsumer<ActorContext, Event> onExitProceeding = (ctx, event) -> {
        if (timer100Trying != null) {
            timer100Trying.cancel();
        }
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
    private final BiConsumer<ActorContext, Event> completed = (ctx, event) -> {
        if (event.isSipMsgEvent()) {
            SipMsgEvent sipEvent = event.toSipMsgEvent();
            SipMessage msg = sipEvent.getSipMessage();

            if (msg.isAck()) {
                become(TransactionState.CONFIRMED);
            } else if (isRetransmittedInvite(msg)) {
                ctx.reverse().forward(lastResponseEvent);
            }
        } else if (event.isSipTimerG()) {
            ++this.timerGCount;
            ctx.scheduler().schedule(calculateNextTimerG(), SipTimer.G);
            ctx.forward(this.lastResponseEvent);
        } else if (event.isSipTimerH()) {
            become(TransactionState.TERMINATED);
        }
    };

    private final BiConsumer<ActorContext, Event> onEnterCompleted = (ctx, event) -> {
        timerG = ctx.scheduler().schedule(calculateNextTimerG(), SipTimer.G);
        timerH = ctx.scheduler().schedule(getTimerConfig().getTimerH(), SipTimer.H);
    };

    private final BiConsumer<ActorContext, Event> onExitCompleted = (ctx, event) -> {
        timerG.cancel();
        timerH.cancel();
    };

    /**
     * The primary purpose of the confirmed state is to absorb any re-transmitted ACKs
     * so therefore we will simply absorb everything in this state and really only
     * react to Timer I, which will take us to the terminated state.
     */
    private final BiConsumer<ActorContext, Event> confirmed = (ctx, event) -> {
        if (event.isSipTimerI()) {
            become(TransactionState.TERMINATED);
        }

        // anything else is absorbed.
    };

    private final BiConsumer<ActorContext, Event> onEnterConfirmed = (ctx, event) -> {
        ctx.scheduler().schedule(getTimerConfig().getTimerI(), SipTimer.I);
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
    private final BiConsumer<ActorContext, Event> accepted = (ctx, event) -> {
        if (event.isSipMsgEvent()) {
            SipMsgEvent sipEvent = event.toSipMsgEvent();
            SipMessage msg = sipEvent.getSipMessage();
            if (msg.isResponse() && msg.toResponse().isSuccess()) {
                // only 2xx responses are forwarded. The rest are consumed.
                // see above state machine
                ctx.forward(event);
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
    private final BiConsumer<ActorContext, Event> onEnterAccepted = (ctx, event) -> {
        final Duration l = getTimerConfig().getTimerL();
        timerL = ctx.scheduler().schedule(l, SipTimer.L);
    };

    private final BiConsumer<ActorContext, Event> onExitAccepted = (ctx, event) -> {
        timerL.cancel();
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


    /**
     * While in the completed state we will re-transmit the final response (which then must be a
     * error response only otherwise we wouldn't be in the completed state to begin with) every time
     * this timer fires.
     * 
     * @return
     */
    private Duration calculateNextTimerG() {
        final long defaultTimerG = timers.getTimerG().toMillis();
        final long t2 = timers.getT2().toMillis();
        return ActorUtils.calculateBackoffTimer(this.timerGCount, defaultTimerG, t2);
    }

    private void relayResponse(final ActorContext ctx, final SipMsgEvent event) {
        if (this.lastResponseEvent == null
                || this.lastResponseEvent.getSipMessage().toResponse().getStatus() < event.getSipMessage().toResponse()
                .getStatus()) {
            this.lastResponseEvent = event;
        }
        ctx.forward(event);
    }

    /**
     * The init state. Just make sure that the first event we receive is the same INVITE as created
     * the transaction (yes, we compare references in this case, that's what we want) and then
     * transition over to the proceeding state.
     */
    private final BiConsumer<ActorContext, Event> init = (ctx, event) -> {
        if (event == getInitialInviteEvent()) {
            ctx.forward(event);
        } else {
            System.err.println("Queue??? shouldn't be able to happen");
        }
        become(TransactionState.PROCEEDING);
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
