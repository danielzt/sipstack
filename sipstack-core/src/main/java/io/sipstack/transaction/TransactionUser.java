package io.sipstack.transaction;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionUser {

    void onRequest(Transaction transaction, SipRequest request);

    void onResponse(Transaction transaction, SipResponse response);

    void onTransactionTerminated(Transaction transaction);
}
