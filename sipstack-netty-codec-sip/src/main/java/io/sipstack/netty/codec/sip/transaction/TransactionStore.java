package io.sipstack.netty.codec.sip.transaction;

import io.pkts.packet.sip.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionStore {

    /**
     * Get or create method.
     *
     * @param msg
     * @return
     */
    TransactionActor ensureTransaction(SipMessage msg);

    TransactionActor get(TransactionId id);

    void remove(TransactionId id);

}
