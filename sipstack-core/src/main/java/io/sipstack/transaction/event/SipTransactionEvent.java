package io.sipstack.transaction.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipTransactionEvent extends TransactionEvent {

    SipMessage message();

    default SipRequest request() {
        return message().toRequest();
    }

    default SipResponse response() {
        return message().toResponse();
    }

    @Override
    default boolean isSipTransactionEvent() {
        return true;
    }

    @Override
    default SipTransactionEvent toSipTransactionEvent() {
        return this;
    }
}
