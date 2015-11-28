package io.sipstack.transport.event.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.sipstack.transport.Flow;
import io.sipstack.transport.event.SipRequestBuilderFlowEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipRequestBuilderFlowEventImpl extends FlowEventImpl implements SipRequestBuilderFlowEvent {

    private final SipMessage.Builder<SipRequest> builder;

    public SipRequestBuilderFlowEventImpl(Flow flow, SipMessage.Builder<SipRequest> builder) {
        super(flow);
        this.builder = builder;
    }

    @Override
    public SipMessage.Builder<SipRequest> getBuilder() {
        return builder;
    }

}
