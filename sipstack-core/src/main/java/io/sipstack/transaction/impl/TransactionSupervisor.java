/**
 * 
 */
package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.actor.Actor;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.Supervisor;
import io.sipstack.event.Event;
import io.sipstack.event.SipEvent;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jonas@jonasborjesson.com
 * 
 */
public class TransactionSupervisor implements Actor, Supervisor {

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

        final TransactionActor newTransaction = TransactionActor.create(this, id, event);
        this.transactions.put(id, newTransaction);
        return newTransaction;
    }

    @Override
    public void onUpstreamEvent(final ActorContext ctx, final Event event) {
        if (event instanceof SipEvent) {
            final SipEvent sipEvent = (SipEvent) event;
            final SipMessage msg = sipEvent.getSipMessage();
            final TransactionId id = TransactionId.create(msg);
            final TransactionActor t = ensureTransaction(id, sipEvent);
            ctx.replace(t);
            ctx.fireUpstreamEvent(event);
        }
    }

    @Override
    public void onDownstreamEvent(final ActorContext ctx, final Event event) {
        // TODO Auto-generated method stub
        System.err.println("[TransactionSupervisor] Got a downstream event, now what????");
    }

    @Override
    public Supervisor getSupervisor() {
        // we are a supervisor so we don't have one ourselves.
        return null;
    }

    @Override
    public void killChild(final Actor actor) {
        // can only be a TransactionActor
        try {
            final TransactionId id = ((TransactionActor) actor).getTransactionId();
            final TransactionActor transaction = this.transactions.remove(id);
            if (transaction != null) {
                System.err.println("Killed off the transaction...");
            }
        } catch (final ClassCastException e) {
            // strange...
            throw e;
        }
    }

}
