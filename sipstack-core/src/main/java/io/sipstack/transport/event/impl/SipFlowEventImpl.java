package io.sipstack.transport.event.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.transport.Flow;
import io.sipstack.transport.event.SipFlowEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipFlowEventImpl extends FlowEventImpl implements SipFlowEvent {

    private final SipMessage msg;

    public SipFlowEventImpl(final Flow flow, final SipMessage msg) {
        super(flow);
        this.msg = msg;
    }

    public SipMessage message() {
        return msg;
    }

}
