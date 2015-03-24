/**
 * 
 */
package io.sipstack.transaction.impl;

import io.netty.util.Timeout;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.Supervisor;
import io.sipstack.config.TimersConfiguration;
import io.sipstack.event.Event;
import io.sipstack.event.SipEvent;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
public class InviteServerTransactionActor implements TransactionActor {

    private static final Logger logger = LoggerFactory.getLogger(InviteServerTransactionActor.class);

    private final TransactionId id;
    private final SipEvent invite;
    private TransactionState state;

    private final TransactionSupervisor parent;

    /**
     * If we receive any re-transmissions, we will send out the last response again if there is
     * one..
     */
    private SipEvent lastResponseEvent;

    /**
     * Our behavior. This will change as we transition from one state to another.
     */
    private BiConsumer<ActorContext, Event> receive;

    /**
     * Contains all the states and their corresponding function pointers. Makes it for easy
     * transition to new behavior as the state changes.
     */
    private final BiConsumer<ActorContext, Event>[] states = new BiConsumer[6];

    /**
     * If we enter the completed state we will setup Timer G for response retransmissions if this
     * transaction is over an unreliable transport.
     */
    private Optional<Timeout> timerG = Optional.empty();

    /**
     * How many times Timer G has fired.
     */
    private final int timerGCount = 0;

    /**
     * If we enter the completed state, Timer H will take us from completed to terminated unless we
     * do receive an ACK before then. Hence, Timer H determines the longest amount of time we can
     * stay in the completed state.
     */
    private final Optional<Timeout> timerH = Optional.empty();

    /**
     * 
     */
    protected InviteServerTransactionActor(final TransactionSupervisor parent, final TransactionId id,
            final SipEvent inviteEvent) {
        this.parent = parent;
        this.id = id;
        this.invite = inviteEvent;

        this.states[TransactionState.INIT.ordinal()] = this.init;
        this.states[TransactionState.PROCEEDING.ordinal()] = this.proceeding;
        this.states[TransactionState.COMPLETED.ordinal()] = this.completed;
        this.states[TransactionState.CONFIRMED.ordinal()] = this.confirmed;
        this.states[TransactionState.TERMINATED.ordinal()] = this.terminated;
        become(TransactionState.INIT);
    }

    private void become(final TransactionState state) {
        logger.info("{} {} -> {}", this.id, this.state, state);
        this.state = state;
        this.receive = this.states[state.ordinal()];
    }

    private final BiConsumer<ActorContext, Event> terminated = (ctx, event) -> {
        System.out.println("in terminated state");
    };

    private final BiConsumer<ActorContext, Event> completed = (ctx, event) -> {
        System.out.println("in completed state");
    };

    private final BiConsumer<ActorContext, Event> confirmed = (ctx, event) -> {
        System.out.println("in confirmed state");
    };

    /**
     * The proceeding state.
     */
    private final BiConsumer<ActorContext, Event> proceeding = (ctx, event) -> {
        if (event.isSipEvent()) {
            final SipEvent sipEvent = (SipEvent) event;
            final SipMessage msg = sipEvent.getSipMessage();
            if (msg.isRequest()) {
                // better be a re-transmission of the invite.
                // TODO: check
                return;
            }

            final SipResponse response = msg.toResponse();
            relayResponse(ctx, sipEvent);
            if (response.isSuccess()) {
                terminate(ctx);
            } else if (response.isFinal()) {
            // TODO: shouldn't really be Timeout object here. We should give out something else.
                final Timeout timerG = ctx.scheduler().scheduleDownstreamEventOnce(calculateNextTimerG(), event);
                this.timerG = Optional.of(timerG);
                become(TransactionState.COMPLETED);
            }
        } else {

        }
    };

    /**
     * While in the completed state we will re-transmit the final response (which then must be a
     * error response only otherwise we wouldn't be in the completed state to begin with) every time
     * this timer fires.
     * 
     * @return
     */
    private Duration calculateNextTimerG() {
        final TimersConfiguration timers = this.parent.getConfig().getTimers();
        final long defaultTimerG = timers.getTimerG().toMillis();
        final long t2 = timers.getT2().toMillis();
        final long g = Math.min(defaultTimerG * (int) Math.pow(2, this.timerGCount), t2);
        return Duration.ofMillis(g);
    }

    private void terminate(final ActorContext ctx) {
        become(TransactionState.TERMINATED);
        ctx.killMe();
    }

    private void relayResponse(final ActorContext ctx, final SipEvent event) {
        if (this.lastResponseEvent == null
                || this.lastResponseEvent.getSipMessage().toResponse().getStatus() < event.getSipMessage().toResponse()
                .getStatus()) {
            this.lastResponseEvent = event;
        }
        ctx.forwardDownstreamEvent(event);
    }

    private void processInitialInvite(final ActorContext ctx) {
        final SipResponse response = this.invite.getSipMessage().createResponse(100);
        if (this.parent.getConfig().isSend100TryingImmediately()) {
            ctx.forwardDownstreamEvent(SipEvent.create(this.invite.key(), response));
        } else {
            final SipEvent event = SipEvent.create(this.invite.key(), response);
            ctx.scheduler().scheduleDownstreamEventOnce(Duration.ofMillis(200), event);
        }
    }

    /**
     * The init state. Just make sure that the first event we receive is the same INVITE as created
     * the transaction (yes, we compare references in this case, that's what we want) and then
     * transition over to the proceeding state.
     */
    private final BiConsumer<ActorContext, Event> init = (ctx, event) -> {
        if (event == this.invite) {
            processInitialInvite(ctx);
            ctx.forwardUpstreamEvent(event); // this must not be processed until we are done
        } else {
            System.err.println("Queue??? shouldn't be able to happen");
        }
        become(TransactionState.PROCEEDING);
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpstreamEvent(final ActorContext ctx, final Event event) {
        this.receive.accept(ctx, event);
    }

    @Override
    public void onDownstreamEvent(final ActorContext ctx, final Event event) {
        this.receive.accept(ctx, event);
    }

    @Override
    public Transaction getTransaction() {
        return new ServerTransactionImpl(this.id, this.state);
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
