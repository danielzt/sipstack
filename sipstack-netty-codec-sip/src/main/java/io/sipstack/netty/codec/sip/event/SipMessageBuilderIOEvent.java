package io.sipstack.netty.codec.sip.event;

import io.pkts.packet.sip.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipMessageBuilderIOEvent extends IOEvent {

    @Override
    default boolean isSipMessageBuilderIOEvent() {
        return true;
    }

    @Override
    default SipMessageBuilderIOEvent toSipMessageBuilderIOEvent() {
        return this;
    }

    SipMessage.Builder<? extends SipMessage> getBuilder();
}
