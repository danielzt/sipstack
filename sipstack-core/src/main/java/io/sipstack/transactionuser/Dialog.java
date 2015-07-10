package io.sipstack.transactionuser;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;

/**
 * @author ajansson@twilio.com
 */
public interface Dialog {
    void send(SipRequest.Builder message);

    void send(SipResponse message);

    SipRequest.Builder createAck();

    SipRequest.Builder createBye();
}
