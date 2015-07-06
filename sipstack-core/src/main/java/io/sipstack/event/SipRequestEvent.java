package io.sipstack.event;

import io.pkts.packet.sip.SipRequest;

/**
 * @author jonas@jonasborjesson.com
 */
public final class SipRequestEvent extends Event {

    private final SipRequest request;

    public SipRequestEvent(final SipRequest request) {
        this.request = request;
    }

    @Override
    public boolean isSipRequestEvent() {
        return true;
    }

    @Override
    public SipRequestEvent toSipRequestEvent() {
        return this;
    }

    @Override
    public boolean isSipEvent() {
        return true;
    }

    @Override
    public SipRequest request() {
        return request;
    }
}
