package io.sipstack.transaction.event.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.event.SipTransactionEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipTransactionEventImpl extends TransactionEventImpl implements SipTransactionEvent {

    private final SipMessage msg;

    public SipTransactionEventImpl(final Transaction transaction, final SipMessage msg) {
        super(transaction);
        this.msg = msg;
    }

    @Override
    public final SipMessage message() {
        return msg;
    }
}
