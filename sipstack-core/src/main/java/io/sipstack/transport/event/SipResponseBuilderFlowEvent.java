package io.sipstack.transport.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.transport.Flow;
import io.sipstack.transport.event.impl.SipResponseBuilderFlowEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipResponseBuilderFlowEvent extends SipBuilderFlowEvent {

    @Override
    default boolean isSipResponseBuilderFlowEvent() {
        return true;
    }

    @Override
    default SipResponseBuilderFlowEvent toSipResponseBuilderFlowEvent() {
        return this;
    }

    @Override
    SipMessage.Builder<SipResponse> getBuilder();

    static SipResponseBuilderFlowEvent create(final Flow flow, final SipMessage.Builder<SipResponse> builder) {
        return new SipResponseBuilderFlowEventImpl(flow, builder);
    }
}
