package io.sipstack.application;

import java.util.function.Consumer;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.address.URI;

/**
 * @author ajansson@twilio.com
 */
public interface UA {

    void send(SipRequest.Builder message);

    void send(SipResponse message);

    void addHandler(Consumer<SipMessage> handler);

    SipRequest.Builder createAck();

    SipRequest.Builder createBye();

    interface Builder {

        Builder withTarget(URI target);

        Builder withRequest(SipRequestEvent event);

        UA build();
    }
}

