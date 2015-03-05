/**
 * 
 */
package io.sipstack.transaction;

import io.sipstack.actor.Actor;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.Event;
import io.sipstack.netty.codec.sip.SipMessageEvent;

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

    private TransactionActor ensureTransaction(final TransactionId id, final SipMessageEvent event) {
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
        final SipMessageEvent msg = (SipMessageEvent) event;
        final TransactionId id = TransactionId.create(msg.getMessage());
        final TransactionActor t = ensureTransaction(id, msg);
        ctx.replace(this, t);
        ctx.fireUpstreamEvent(event);
    }

    @Override
    public void onDownstreamEvent(final ActorContext ctx, final Event event) {
        // TODO Auto-generated method stub

    }

}
