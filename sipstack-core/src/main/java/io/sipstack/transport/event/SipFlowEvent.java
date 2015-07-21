package io.sipstack.transport.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipFlowEvent extends FlowEvent {

    @Override
    default boolean isSipFlowEvent() {
        return true;
    }

    @Override
    default SipFlowEvent toSipFlowEvent() {
        return this;
    }

    SipMessage message();

    default SipRequest request() {
        return message().toRequest();
    }

    default SipResponse response() {
        return message().toResponse();
    }
}
