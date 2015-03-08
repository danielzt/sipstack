/**
 * 
 */
package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.Event;
import io.sipstack.actor.SipEvent;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;

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
 */
public class InviteServerTransactionActor implements TransactionActor {

    private final TransactionId id;
    private final SipEvent invite;
    private TransactionState state;

    private BiConsumer<ActorContext, Event> receive;

    /**
     * 
     */
    protected InviteServerTransactionActor(final TransactionId id, final SipEvent inviteEvent) {
        this.id = id;
        this.invite = inviteEvent;
        become(this.init);
    }

    private void become(final BiConsumer<ActorContext, Event> nextState) {
        this.receive = nextState;
    }

    /**
     * The proceeding state
     */
    private final BiConsumer<ActorContext, Event> proceeding = (ctx, event) -> {
        System.err.println("Ok, so I swapped states...");
    };

    private void processInitialInvite(final ActorContext ctx) {
        final SipResponse response = this.invite.getSipMessage().createResponse(100);
        ctx.fireDownstreamEvent(SipEvent.create(this.invite.key(), response));
    }

    /**
     * My init state
     */
    private final BiConsumer<ActorContext, Event> init = (ctx, event) -> {
        System.err.println("in init and processing an event");
        if (event == this.invite) {
            processInitialInvite(ctx);
            ctx.fireUpstreamEvent(event); // this must not be processed until we are done
        } else {
            System.err.println("Queue??? shouldn't be able to happen");
        }
        this.state = TransactionState.PROOCEEDING;
        become(this.proceeding);
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
        // TODO Auto-generated method stub
    }

    @Override
    public Transaction getTransaction() {
        return new ServerTransactionImpl(this.id, this.state);
    }
}
