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
     * @param isUpstream denotes the direction of the message, which is important since we need to know
     *                   whether or not we should create a client or server transaction. If it "isUpstream" (i.e. true)
     *                   then that means that we received the message from the network (since it is coming from the
     *                   lower level parts of the stack traveling "up") and as such, if it is a request we should
     *                   create a server transaction. If false, then it is traveling the opposite direction and as such
     *                   we need to create a client transaction.
     * @param msg
     * @return
     */
    TransactionHolder ensureTransaction(boolean isUpstream, SipMessage msg);

    TransactionHolder get(TransactionId id);

    void remove(TransactionId id);

}
