package io.sipstack.transactionuser;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.transaction.Transaction;

/**
 * @author ajansson@twilio.com
 */
public class TransactionUserEvent {
    private Dialog dialog;
    private Transaction transaction;
    private SipMessage message;

    public TransactionUserEvent(final Dialog dialog, final Transaction transaction, final SipMessage message) {
        this.dialog = dialog;
        this.transaction = transaction;
        this.message = message;
    }

    public Dialog dialog() {
        return dialog;
    }

    public Transaction transaction() {
        return transaction;
    }

    public SipMessage message() {
        return message;
    }
}
