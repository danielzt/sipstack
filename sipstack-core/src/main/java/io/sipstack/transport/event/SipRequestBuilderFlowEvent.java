package io.sipstack.transport.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.sipstack.transport.Flow;
import io.sipstack.transport.event.impl.SipRequestBuilderFlowEventImpl;

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

    static SipRequestBuilderFlowEvent create(final Flow flow, final SipMessage.Builder<SipRequest> builder) {
        return new SipRequestBuilderFlowEventImpl(flow, builder);
    }

}
