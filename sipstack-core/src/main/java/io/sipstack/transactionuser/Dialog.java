package io.sipstack.transactionuser;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;

/**
 * @author ajansson@twilio.com
 */
public interface Dialog {
    void send(SipMessage message);

    SipRequest.Builder createAck();

    SipRequest.Builder createBye();
}
