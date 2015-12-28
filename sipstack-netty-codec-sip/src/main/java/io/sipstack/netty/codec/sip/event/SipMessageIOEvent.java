package io.sipstack.netty.codec.sip.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipMessageIOEvent extends IOEvent {

    default boolean isSipMessageIOEvent() {
        return true;
    }

    default SipMessageIOEvent toSipMessageIOEvent() {
        return this;
    }

    SipMessage message();

    default SipRequest request() {
        return toSipRequestIOEvent().request();
    }

    default SipResponse response() {
        return toSipResponseIOEvent().response();
    }
}
