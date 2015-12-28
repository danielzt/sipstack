package io.sipstack.transport.event;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.transport.Flow;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipBuilderFlowEvent extends FlowEvent {

    default boolean isSipBuilderFlowEvent() {
        return true;
    }

    default SipBuilderFlowEvent toSipBuilderFlowEvent() {
        return this;
    }

    SipMessage.Builder<? extends SipMessage> getBuilder();
}
