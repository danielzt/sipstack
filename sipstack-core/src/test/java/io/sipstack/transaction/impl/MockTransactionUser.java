package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.header.SipHeader;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;
import io.sipstack.transaction.TransactionUser;
import io.sipstack.transaction.Transactions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private Transactions transactions;

    /**
     * A list of all messages we have received...
     */
    private List<SipAndTransactionHolder> messages = new ArrayList<>();

    private Map<TransactionId, Transaction> allTransactions = new ConcurrentHashMap<>();

    private Map<TransactionId, Transaction> terminatedTransaction = new ConcurrentHashMap<>();

    public void start(final Transactions transactions) {
        this.transactions = transactions;
    }

    public void ensureTransactionTerminated(final TransactionId id) {
        final Transaction transaction = terminatedTransaction.remove(id);
        assertThat(transaction, not((Transaction)null));
        assertThat(transaction.state(), is(TransactionState.TERMINATED));
    }

    /**
     * Make sure that there was a SINGLE request with the specified method received by
     * this transaction user.
     *
     * @param method
     */
    public Transaction assertAndConsumeRequest(final String method) {
        synchronized (messages) {
            final Iterator<SipAndTransactionHolder> it = messages.iterator();
            int count = 0;
            Transaction transaction = null;
            while (it.hasNext()) {
                final SipAndTransactionHolder holder = it.next();
                final SipMessage msg = holder.msg;
                if (msg.isRequest() && method.equalsIgnoreCase(msg.getMethod().toString())) {
                    it.remove();
                    transaction = holder.transaction;
                    ++count;
                }
            }

            if (count == 0) {
                fail("Expected a " + method.toUpperCase() + " request but didn't find one");
            } else if (count > 1) {
                fail("Found many " + method.toUpperCase() + " requests");
            }

            return transaction;
        }
    }

    @Override
    public void onRequest(final Transaction transaction, final SipRequest request) {
        recordMessage(transaction, request);

        final SipHeader header = request.getHeader("X-Transaction-Test-Response");
        int responseCode = 200;
        if (header != null) {
            responseCode = Integer.valueOf(header.getValue().toString());
        }

        transactions.send(request.createResponse(responseCode));
    }

    @Override
    public void onResponse(final Transaction transaction, final SipResponse response) {
        recordMessage(transaction, response);
    }

    private void recordMessage(final Transaction transaction, final SipMessage msg) {
        assertThat(transaction, not((Transaction) null));

        synchronized (messages) {
            messages.add(new SipAndTransactionHolder(transaction, msg));
        }

        allTransactions.put(transaction.id(), transaction);
    }

    @Override
    public void onTransactionTerminated(final Transaction transaction) {
        assertThat(transaction, not((Transaction)null));
        assertThat(allTransactions.remove(transaction.id()), not((Transaction)null));
        terminatedTransaction.put(transaction.id(), transaction);
    }

    @Override
    public void onIOException(Transaction transaction, SipMessage msg) {
        assertThat(transaction, not((Transaction)null));
    }

    private static class SipAndTransactionHolder {

        private final SipMessage msg;
        private final Transaction transaction;

        SipAndTransactionHolder(final Transaction transaction, final SipMessage msg) {
            this.transaction = transaction;
            this.msg = msg;
        }

    }

}
