package io.sipstack.transactionuser.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.transaction.Transaction;
import io.sipstack.transactionuser.TransactionEvent;

/**
 * @author ajansson@twilio.com
 */
public class DefaultTransactionEvent implements TransactionEvent {
    private final Transaction tx;
    private final SipMessage message;

    public DefaultTransactionEvent(final Transaction tx, final SipMessage message) {
        this.tx = tx;
        this.message = message;
    }

    @Override
    public Transaction transaction() {
        return tx;
    }

    @Override
    public SipMessage message() {
        return message;
    }
}
