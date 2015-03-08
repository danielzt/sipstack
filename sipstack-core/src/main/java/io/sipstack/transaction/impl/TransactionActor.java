/**
 * 
 */
package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.actor.Actor;
import io.sipstack.actor.SipEvent;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;

/**
 * @author jonas
 *
 */
public interface TransactionActor extends Actor {

    /**
     * The a snapshot of the current transaction as a {@link Transaction} object.
     * 
     * @return
     */
    Transaction getTransaction();

    static TransactionActor create(final TransactionId id, final SipEvent event) {
        final SipMessage msg = event.getSipMessage();
        if (msg.isRequest() && msg.isInvite()) {
            return new InviteServerTransactionActor(id, event);
        }
        return null;
    }

}
