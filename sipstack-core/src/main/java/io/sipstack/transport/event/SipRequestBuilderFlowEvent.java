package io.sipstack.transport.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipRequestBuilderFlowEvent extends SipBuilderFlowEvent {

    @Override
    default boolean isSipRequestBuilderFlowEvent() {
        return true;
    }

    @Override
    default SipRequestBuilderFlowEvent toSipRequestBuilderFlowEvent() {
        return this;
    }

    @Override
    SipMessage.Builder<SipRequest> getBuilder();
}
