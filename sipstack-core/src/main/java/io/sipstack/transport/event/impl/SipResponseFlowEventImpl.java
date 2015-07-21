package io.sipstack.transport.event.impl;

import io.pkts.packet.sip.SipResponse;
import io.sipstack.transport.Flow;
import io.sipstack.transport.event.SipResponseFlowEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipResponseFlowEventImpl extends SipFlowEventImpl implements SipResponseFlowEvent {

    public SipResponseFlowEventImpl(final Flow flow, final SipResponse response) {
        super(flow, response);
    }
}
