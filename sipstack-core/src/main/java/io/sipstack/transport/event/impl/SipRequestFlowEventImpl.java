package io.sipstack.transport.event.impl;

import io.pkts.packet.sip.SipRequest;
import io.sipstack.transport.Flow;
import io.sipstack.transport.event.SipRequestFlowEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipRequestFlowEventImpl extends SipFlowEventImpl implements SipRequestFlowEvent {

    public SipRequestFlowEventImpl(final Flow flow, final SipRequest request) {
        super(flow, request);
    }
}
