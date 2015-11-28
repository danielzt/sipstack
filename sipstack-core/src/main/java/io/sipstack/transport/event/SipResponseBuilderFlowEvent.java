package io.sipstack.transport.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipResponseBuilderFlowEvent extends SipBuilderFlowEvent {

    @Override
    SipMessage.Builder<SipResponse> getBuilder();

}
