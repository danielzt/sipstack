package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.header.SipHeader;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionUser;
import io.sipstack.transaction.Transactions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockTransactionUser implements TransactionUser {

    /**
     * The {@link Transactions} API is our way into the transaction layer
     * and is how we will be writing messages back.
     */
    private Transactions transactionLayer;

    private SipAndTransactionStorage storage = new SipAndTransactionStorage();

    private Map<TransactionId, Transaction> allTransactions = new ConcurrentHashMap<>();

    private Map<TransactionId, Transaction> terminatedTransaction = new ConcurrentHashMap<>();

    public void ensureTransactionTerminated(final TransactionId id) {
        storage.ensureTransactionTerminated(id);
    }

    public Transaction assertAndConsumeRequest(final String method) {
        return storage.assertAndConsumeRequest(method);
    }

    public Transaction assertAndConsumeResponse(final String method, final int responseStatus) {
        return storage.assertAndConsumeResponse(method, responseStatus);
    }

    /**
     * Poke this client to send out a new request through the transaction layer.
     *
     * @param request the request to send.
     * @return the transaction that got created for this request.
     */
    public Transaction sendRequest(final SipRequest request) {
        final Transaction transaction = transactionLayer.send(request);
        storage.store(transaction, request);
        return transaction;
    }

    public void reset() {
        storage.reset();
    }

    @Override
    public void init(final Transactions transactionLayer) {
        this.transactionLayer = transactionLayer;
    }

    @Override
    public void onRequest(final Transaction transaction, final SipRequest request) {
        storage.store(transaction, request);

        final Optional<SipHeader> header = request.getHeader("X-Transaction-Test-Response");
        int responseCode = 200;
        if (header.isPresent()) {
            responseCode = Integer.valueOf(header.get().getValue().toString());
        }

        transactionLayer.send(request.createResponse(responseCode));
    }

    @Override
    public void onResponse(final Transaction transaction, final SipResponse response) {
        storage.store(transaction, response);
    }

    @Override
    public void onTransactionTerminated(final Transaction transaction) {
        storage.onTransactionTerminated(transaction);
    }

    @Override
    public void onIOException(Transaction transaction, SipMessage msg) {
        assertThat(transaction, not((Transaction)null));
    }

}
