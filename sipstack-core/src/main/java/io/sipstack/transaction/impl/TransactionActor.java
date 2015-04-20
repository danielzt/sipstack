/**
 * 
 */
package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.actor.Actor;
import io.sipstack.event.SipMsgEvent;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public interface TransactionActor extends Actor {

    /**
     * The a snapshot of the current transaction as a {@link Transaction} object.
     * 
     * @return
     */
    Transaction getTransaction();

    TransactionId getTransactionId();

    static TransactionActor create(final TransactionSupervisor parent, final TransactionId id, final SipMsgEvent event) {
        final SipMessage msg = event.getSipMessage();
        if (msg.isRequest()) {
            if (msg.isInvite()) {
                return new InviteServerTransactionActor(parent, id, event);
            }
            return new NonInviteServerTransactionActor(parent, id, event);
        }
        return null;
    }

}
