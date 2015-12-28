package io.sipstack.netty.codec.sip.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipResponseBuilderIOEvent extends SipMessageBuilderIOEvent {

    @Override
    default boolean isSipResponseBuilderIOEvent() {
        return true;
    }

    @Override
    default SipResponseBuilderIOEvent toSipResponseBuilderIOEvent() {
        return this;
    }

    @Override
    SipMessage.Builder<SipResponse> getBuilder();
}
