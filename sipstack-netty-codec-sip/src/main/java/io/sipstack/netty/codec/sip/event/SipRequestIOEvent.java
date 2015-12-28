package io.sipstack.netty.codec.sip.event;

import io.pkts.packet.sip.SipRequest;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipRequestIOEvent extends SipMessageIOEvent {

    default boolean isSipRequestIOEvent() {
        return true;
    }

    default SipRequestIOEvent toSipRequestIOEvent() {
        return this;
    }

    @Override
    default SipRequest request() {
        return message().toRequest();
    }
}
