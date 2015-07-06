package io.sipstack.transaction;

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
    TransactionHolder ensureTransaction(SipMessage msg);

    TransactionHolder get(TransactionId id);

    void remove(TransactionId id);

}
