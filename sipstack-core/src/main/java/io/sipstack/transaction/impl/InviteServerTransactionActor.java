/**
 * 
 */
package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.Supervisor;
import io.sipstack.event.Event;
import io.sipstack.event.SipEvent;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;

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
public class InviteServerTransactionActor implements TransactionActor {

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
                become(TransactionState.COMPLETED);
            }
        } else {

        }
    };

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
            System.err.println("Ok, so i am scheduling it for later...");
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
