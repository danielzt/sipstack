package io.sipstack.transaction.event.impl;

import io.pkts.packet.sip.SipResponse;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.event.SipResponseTransactionEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipResponseTransactionEventImpl extends SipTransactionEventImpl implements SipResponseTransactionEvent {

    public SipResponseTransactionEventImpl(final Transaction transaction, final SipResponse response) {
        super(transaction, response);
    }
}
