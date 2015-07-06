package io.sipstack.transaction;

import io.pkts.packet.sip.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Transaction {

    TransactionId id();

    /**
     * Send the message in the context of this transaction.
     *
     * The message will be sent asynchronously so when this method
     * returns the state of the {@link Transaction} has not changed
     * yet.
     *
     * @param msg
     */
    void send(SipMessage msg);
}
