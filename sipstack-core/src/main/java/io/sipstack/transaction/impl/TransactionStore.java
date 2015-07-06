package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.transaction.TransactionId;

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
