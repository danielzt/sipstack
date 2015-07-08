package io.sipstack.transactionuser;

import java.util.function.Consumer;

import io.pkts.packet.sip.SipMessage;

/**
 * @author ajansson@twilio.com
 */
public interface Dialog {
    /**
     * Dialog id.
     */
    String id();

    void setConsumer(final Consumer<TransactionUserEvent> consumer);

    void send(final SipMessage message);
}
