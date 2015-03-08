/**
 * 
 */
package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.actor.Actor;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.Event;
import io.sipstack.actor.SipEvent;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jonas@jonasborjesson.com
 * 
 */
public class TransactionSupervisor implements Actor {

    private final Map<TransactionId, TransactionActor> transactions = new HashMap<>(100, 0.75f);

    /**
     * 
     */
    public TransactionSupervisor() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Get the {@link Transaction} for the given {@link TransactionId}.
     * 
     * Note: this can ONLY be called from the thread that normally handles transactions for this
     * supervisor. Calling this from another thread is not safe. Hence, ONLY the internal
     * implementation to sipstack should be using this method.
     * 
     * @param id
     * @return
     */
    public Transaction getTransaction(final TransactionId id) {
        final TransactionActor t = this.transactions.get(id);
        if (t != null) {
            return t.getTransaction();
        }

        return null;
    }

    private TransactionActor ensureTransaction(final TransactionId id, final SipEvent event) {
        final TransactionActor t = this.transactions.get(id);
        if (t != null) {
            return t;
        }

        final TransactionActor newTransaction = TransactionActor.create(id, event);
        this.transactions.put(id, newTransaction);
        return newTransaction;
    }

    @Override
    public void onUpstreamEvent(final ActorContext ctx, final Event event) {
        if (event.isSipMessage()) {
            final SipEvent sipEvent = (SipEvent) event;
            final SipMessage msg = sipEvent.getSipMessage();
            final TransactionId id = TransactionId.create(msg);
            final TransactionActor t = ensureTransaction(id, sipEvent);
            ctx.replace(this, t);
            ctx.fireUpstreamEvent(event);
        }
    }

    @Override
    public void onDownstreamEvent(final ActorContext ctx, final Event event) {
        // TODO Auto-generated method stub
        System.err.println("[TransactionSupervisor] Got a downstream event, now what????");
        ctx.fireDownstreamEvent(event);
    }

}
