package io.sipstack.transactionuser;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.transaction.Transaction;

/**
 * @author ajansson@twilio.com
 */
public interface TransactionEvent {
    Transaction transaction();

    SipMessage message();
}
