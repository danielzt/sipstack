package io.sipstack.transactionuser;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionUser;
import io.sipstack.transaction.Transactions;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultTransactionUser implements TransactionUser {

    private Transactions transactions;

    public DefaultTransactionUser() {

    }

    public void start(final Transactions transactions) {
        this.transactions = transactions;
    }

    @Override
    public void onRequest(Transaction transaction, SipRequest request) {
        if (!request.isAck()) {
            transactions.send(transaction.flow(), request.createResponse(200));
        }
    }

    @Override
    public void onResponse(Transaction transaction, SipResponse response) {

    }

    @Override
    public void onTransactionTerminated(Transaction transaction) {

    }

    @Override
    public void onIOException(Transaction transaction, SipMessage msg) {

    }
}
