package io.sipstack.transport.event.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.transport.Flow;
import io.sipstack.transport.event.SipRequestBuilderFlowEvent;
import io.sipstack.transport.event.SipResponseBuilderFlowEvent;
import io.sipstack.transport.event.impl.FlowEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipResponseBuilderFlowEventImpl extends FlowEventImpl implements SipResponseBuilderFlowEvent {

    private final SipMessage.Builder<SipResponse> builder;

    public SipResponseBuilderFlowEventImpl(Flow flow, SipMessage.Builder<SipResponse> builder) {
        super(flow);
        this.builder = builder;
    }

    @Override
    public SipMessage.Builder<SipResponse> getBuilder() {
        return builder;
    }

}
