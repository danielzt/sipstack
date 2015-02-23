/**
 * 
 */
package io.sipstack.transaction;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.actor.Actor;
import io.sipstack.netty.codec.sip.SipMessageEvent;

/**
 * @author jonas
 *
 */
public interface TransactionActor extends Actor {

    static TransactionActor create(final TransactionId id, final SipMessageEvent event) {
        final SipMessage msg = event.getMessage();
        if (msg.isRequest() && msg.isInvite()) {
            return new InviteServerTransactionActor(id, event);
        }
        return null;
    }

}
