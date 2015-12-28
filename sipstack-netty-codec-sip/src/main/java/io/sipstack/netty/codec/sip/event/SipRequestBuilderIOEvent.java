package io.sipstack.netty.codec.sip.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipRequestBuilderIOEvent extends SipMessageBuilderIOEvent {

    @Override
    default boolean isSipRequestBuilderIOEvent() {
        return true;
    }

    @Override
    default SipRequestBuilderIOEvent toSipRequestBuilderIOEvent() {
        return this;
    }

    @Override
    SipMessage.Builder<SipRequest> getBuilder();
}
