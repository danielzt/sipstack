/**
 * 
 */
package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.ActorUtils;
import io.sipstack.actor.Scheduler;
import io.sipstack.actor.Supervisor;
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
public class InviteServerTransactionActorNoLambda implements TransactionActor {

    private static final Logger logger = LoggerFactory.getLogger(InviteServerTransactionActorNoLambda.class);

    private final TransactionId id;

    private final SipMsgEvent invite;

    private final TransactionSupervisor parent;

    private final TimersConfiguration timers;

    private final TransactionLayerConfiguration transactionConfig;

    private ActorContext currentCtx;

    private Event currentEvent;

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

    private TransactionState currentState;

    /**
     *
     */
    protected InviteServerTransactionActorNoLambda(final TransactionSupervisor parent, final TransactionId id,
                                                   final SipMsgEvent inviteEvent) {
        this.timers = parent.getConfig().getTimers();
        this.transactionConfig = parent.getConfig();
        this.parent = parent;
        this.id = id;
        this.invite = inviteEvent;
        currentState = TransactionState.INIT;
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
    private void onProceeding(ActorContext ctx, Event event) {
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
                // become(TransactionState.ACCEPTED);
                become(TransactionState.TERMINATED);
            } else if (response.isFinal()) {
                become(TransactionState.COMPLETED);
            }
        } else if (event.isSipTimer100Trying()) {
            // Note, we have to reverse the context because when we
            // scheduled the 100 Trying event we had an 'upstream' context
            // since we were processing an incoming INVITE at that time.
            final SipResponse response = invite.getSipMessage().createResponse(100);
            ctx.reverse().forward(SipMsgEvent.create(invite.key(), response));
        } else {

        }
    }

    protected final void become(final TransactionState newState) {
        // logger().info("{} {} -> {}", this.id, currentState, newState);

        if (currentState != newState) {
            onExit(currentState);;
            onEnter(newState);;
        }

        currentState = newState;
    }

    @Override
    public final void onEvent(ActorContext ctx, Event event) {
        currentCtx = ctx;
        currentEvent = event;
        switch (currentState) {
            case INIT:
                onInit(ctx, event);
                break;
            case PROCEEDING:
                onProceeding(ctx, event);
                break;
            case COMPLETED:
                onCompleted(ctx, event);
                break;
            case ACCEPTED:
                onAccepted(ctx, event);
                break;
            case CONFIRMED:
                onConfirmed(ctx, event);
                break;
            case TERMINATED:
                onTerminated(ctx, event);
                break;
            default:
                throw new RuntimeException("strange, unknown state...");
        }
    }

    public final void onEnter(final TransactionState state) {
        switch (state) {
            case PROCEEDING:
                onEnterProceeding(currentCtx, currentEvent);
                break;
            case COMPLETED:
                onEnterCompleted(currentCtx, currentEvent);
                break;
            case ACCEPTED:
                onEnterAccepted(currentCtx, currentEvent);
                break;
            case CONFIRMED:
                onEnterConfirmed(currentCtx, currentEvent);
                break;
            case TERMINATED:
                onEnterTerminated(currentCtx, currentEvent);
                break;
        }
    }

    public final void onExit(final TransactionState state) {
        switch (state) {
            case PROCEEDING:
                onExitProceeding(currentCtx, currentEvent);
                break;
            case COMPLETED:
                onExitCompleted(currentCtx, currentEvent);
                break;
            case ACCEPTED:
                onExitAccepted(currentCtx, currentEvent);
                break;
        }
    }

    /**
     * When entering the Proceeding state we may send a 100 Trying right away (depending on configuration) or we may
     * delay it with 200 ms and send it later unless TU already have sent some kind of response (any kind really)
     */
    private void onEnterProceeding(final ActorContext ctx, final Event event) {
        if (transactionConfig.isSend100TryingImmediately()) {
            final SipResponse response = invite.getSipMessage().createResponse(100);
            ctx.reverse().forward(SipMsgEvent.create(invite.key(), response));
        } else {
            timer100Trying = ctx.scheduler().schedule(Duration.ofMillis(200), SipTimer.Trying);
        }
    }

    private void onExitProceeding(final ActorContext ctx, final Event event) {
        if (timer100Trying != null) {
            timer100Trying.cancel();
        }
    }


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
    private void onCompleted(ActorContext ctx, Event event) {
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
    }

    private void onEnterCompleted(ActorContext ctx, Event event) {
        timerG = ctx.scheduler().schedule(calculateNextTimerG(), SipTimer.G);
        timerH = ctx.scheduler().schedule(timers.getTimerH(), SipTimer.H);
    };

    private void onExitCompleted(ActorContext ctx, Event event) {
        timerG.cancel();
        timerH.cancel();
    }

    /**
     * The primary purpose of the confirmed state is to absorb any re-transmitted ACKs
     * so therefore we will simply absorb everything in this state and really only
     * react to Timer I, which will take us to the terminated state.
     */
    private void onConfirmed(final ActorContext ctx, final Event event) {
        if (event.isSipTimerI()) {
            become(TransactionState.TERMINATED);
        }

        // anything else is absorbed.
    }

    private void onEnterConfirmed(final ActorContext ctx, final Event event) {
        ctx.scheduler().schedule(timers.getTimerI(), SipTimer.I);
    }


    /**
     *
     * Implements the proceeding state as follows:
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
    private void onAccepted(final ActorContext ctx, final Event event) {
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
    }

    /**
     *
     */
    private void onEnterAccepted(final ActorContext ctx, final Event event) {
        final Duration l = timers.getTimerL();
        timerL = ctx.scheduler().schedule(l, SipTimer.L);
    }

    private void onExitAccepted(final ActorContext ctx, final Event event) {
        timerL.cancel();
    }

    /**
     * Implements the state terminated which really doesn't do anything at all.
     */
    private void onTerminated(final ActorContext ctx, final Event event) {
        // left empty intentionally
    }

    /**
     * Implements the state terminated which really doesn't do much other then dies...
     */
    private void onEnterTerminated(final ActorContext ctx, final Event event) {
        ctx.killMe();;
    }


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
    private void onInit(final ActorContext ctx, final Event event) {
        if (event == invite) {
            ctx.forward(event);
        } else {
            System.err.println("Queue??? shouldn't be able to happen");
        }
        become(TransactionState.PROCEEDING);
    };

    @Override
    public Transaction getTransaction() {
        return new ServerTransactionImpl(this.id, currentState);
    }

    @Override
    public Supervisor getSupervisor() {
        return this.parent;
    }

    @Override
    public TransactionId getTransactionId() {
        return this.id;
    }

}
