package io.sipstack.application.impl;

import io.pkts.packet.sip.SipResponse;
import io.sipstack.application.SipResponseEvent;
import io.sipstack.transaction.Transaction;

/**
 * @author ajansson@twilio.com
 */
public class DefaultSipResponseEvent implements SipResponseEvent {
    private final Transaction tx;
    private final SipResponse response;

    public DefaultSipResponseEvent(final Transaction tx, final SipResponse response) {
        this.tx = tx;
        this.response = response;
    }

    public Transaction transaction() {
        return tx;
    }

    @Override
    public SipResponse message() {
        return response;
    }
}
