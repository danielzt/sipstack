package io.sipstack.transactionuser;

import java.util.function.Consumer;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;

/**
 * @author ajansson@twilio.com
 */
public interface Dialog {
    /**
     * Dialog id.
     */
    String id();

    Consumer<TransactionUserEvent> getConsumer();

    void setConsumer(Consumer<TransactionUserEvent> consumer);

    void send(SipMessage message);

    SipRequest.Builder createAck();
}
