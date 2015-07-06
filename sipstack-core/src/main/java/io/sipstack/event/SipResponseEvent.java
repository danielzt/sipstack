package io.sipstack.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipResponseEvent extends Event {

    private final SipResponse response;

    public SipResponseEvent(final SipResponse response) {
        this.response = response;
    }

    @Override
    public boolean isSipResponseEvent() {
        return true;
    }

    @Override
    public SipResponseEvent toSipResponseEvent() {
        return this;
    }

    @Override
    public boolean isSipEvent() {
        return true;
    }

    @Override
    public SipResponse response() {
        return response;
    }

    @Override
    public SipMessage getSipMessage() {
        return response;
    }
}

