package io.sipstack.application.impl;

import io.pkts.packet.sip.SipRequest;
import io.sipstack.application.SipRequestEvent;
import io.sipstack.transaction.Transaction;

/**
 * @author ajansson@twilio.com
 */
public class DefaultSipRequestEvent implements SipRequestEvent {
    private final Transaction tx;
    private final SipRequest request;

    public DefaultSipRequestEvent(final Transaction tx, final SipRequest request) {
        this.tx = tx;
        this.request = request;
    }

    public Transaction transaction() {
        return tx;
    }

    @Override
    public SipRequest message() {
        return request;
    }
}
