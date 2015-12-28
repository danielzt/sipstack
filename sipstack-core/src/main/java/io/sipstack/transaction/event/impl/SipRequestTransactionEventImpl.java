package io.sipstack.transaction.event.impl;

import io.pkts.packet.sip.SipRequest;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.event.SipRequestTransactionEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipRequestTransactionEventImpl extends SipTransactionEventImpl implements SipRequestTransactionEvent {

    public SipRequestTransactionEventImpl(final Transaction transaction,final SipRequest request) {
        super(transaction, request);
    }
}
