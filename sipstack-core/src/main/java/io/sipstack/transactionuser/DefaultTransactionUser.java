package io.sipstack.transactionuser;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionLayer;
import io.sipstack.transaction.TransactionUser;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultTransactionUser implements TransactionUser {

    private final TransactionLayer transactionLayer;

    public DefaultTransactionUser(final TransactionLayer transactionLayer) {
        this.transactionLayer = transactionLayer;
    }

    @Override
    public void onRequest(Transaction transaction, SipRequest request) {
        if (!request.isAck()) {
            transaction.send(request.createResponse(200));
        }
    }

    @Override
    public void onResponse(Transaction transaction, SipResponse response) {

    }

    @Override
    public void onTransactionTerminated(Transaction transaction) {

    }
}
