package io.sipstack.transaction;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;

/**
 * This interface represents the user of the transaction layer, which in SIP is (surprise surprise)
 * called the Transaction User. The
 * @author jonas@jonasborjesson.com
 */
public interface TransactionUser {

    void start(TransactionLayer transactionLayer);

    void onRequest(Transaction transaction, SipRequest request);

    void onResponse(Transaction transaction, SipResponse response);

    void onTransactionTerminated(Transaction transaction);

    /**
     * If the underlying stack fails to actually send the message, for whatever reason,
     * this method will be called with details about the failure.
     *
     * @param transaction
     * @param msg
     */
    void onIOException(Transaction transaction, SipMessage msg);
}
