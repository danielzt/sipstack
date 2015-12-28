package io.sipstack.netty.codec.sip.event;

import io.pkts.packet.sip.SipResponse;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipResponseIOEvent extends SipMessageIOEvent {

    default boolean isSipResponseIOEvent() {
        return true;
    }

    default SipResponseIOEvent toSipResponseIOEvent() {
        return this;
    }

    @Override
    default SipResponse response() {
        return message().toResponse();
    }
}
