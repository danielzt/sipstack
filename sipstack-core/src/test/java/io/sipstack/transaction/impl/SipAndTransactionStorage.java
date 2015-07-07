package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

/**
 * Helper class for unit tests that needs to consume sip messages and transactions.
 * It offer up storage as well as helper method for checking that a particular
 * message indeed was received etc.
 *
 * @author jonas@jonasborjesson.com
 */
public class SipAndTransactionStorage {

    /**
     * A list of all messages we have received...
     */
    private List<Holder> messages = new ArrayList<>();

    private Map<TransactionId, Transaction> allTransactions = new ConcurrentHashMap<>();

    private Map<TransactionId, Transaction> terminatedTransaction = new ConcurrentHashMap<>();

    public void reset() {
        synchronized (messages) {
            messages.clear();
        }
        allTransactions.clear();
        terminatedTransaction.clear();
    }

    public void store(final SipMessage msg) {
        synchronized (messages) {
            messages.add(new Holder(null, msg));
        }
    }

    public void store(final Transaction transaction, final SipMessage msg) {
        assertThat(transaction, not((Transaction) null));

        synchronized (messages) {
            messages.add(new Holder(transaction, msg));
        }

        allTransactions.put(transaction.id(), transaction);
    }

    /**
     * Ensure that the message was received/sent but do NOT consume it.
     *
     * @param method
     * @return
     */
    public SipRequest assertRequest(final String method) {
        final Holder holder = assertRequest(false, method);
        return holder.msg.toRequest();
    }

    public Transaction consumeRequest(final SipRequest request) {
        final Holder holder = assertRequest(true, request.getMethod().toString());
        return holder.transaction;
    }

    /**
     * Make sure that there was a SINGLE request with the specified method received by
     * this transaction user.
     *
     * @param method
     */
    public Transaction assertAndConsumeRequest(final String method) {
        final Holder holder = assertRequest(true, method);
        return holder.transaction;
    }

    private Holder assertRequest(final boolean consume, final String method) {
        synchronized (messages) {
            final Iterator<Holder> it = messages.iterator();
            int count = 0;
            Holder holder = null;
            while (it.hasNext()) {
                holder = it.next();
                final SipMessage msg = holder.msg;
                if (msg.isRequest() && method.equalsIgnoreCase(msg.getMethod().toString())) {
                    if (consume) {
                        it.remove();
                    }
                    ++count;
                }
            }

            if (count == 0) {
                fail("Expected a " + method.toUpperCase() + " request but didn't find one");
            } else if (count > 1) {
                fail("Found many " + method.toUpperCase() + " requests");
            }

            return holder;
        }
    }

    public Transaction assertAndConsumeResponse(final String method, final int responseStatus) {

        synchronized (messages) {
            final Iterator<Holder> it = messages.iterator();
            int count = 0;
            Holder holder = null;
            while (it.hasNext()) {
                holder = it.next();
                final SipMessage msg = holder.msg;
                if (msg.isResponse()
                        && method.equalsIgnoreCase(msg.getMethod().toString())
                        && msg.toResponse().getStatus() == responseStatus) {
                    it.remove();
                    ++count;
                }
            }

            if (count == 0) {
                fail("Expected a " + responseStatus + " response to " + method.toUpperCase() + " but didn't find one");
            } else if (count > 1) {
                fail("Found many " + responseStatus + " response to " + method.toUpperCase() + " but didn't find one");
            }

            return holder.transaction;
        }
    }

    /**
     * Call this method when a transaction has been terminated. Your test mock class will be called
     * on the {@link MockTransactionUser#onTransactionTerminated(Transaction)} so then you should
     * just call this method.
     *
     * @param transaction
     */
    public void onTransactionTerminated(final Transaction transaction) {
        assertThat(transaction, not((Transaction)null));
        assertThat(allTransactions.remove(transaction.id()), not((Transaction)null));
        terminatedTransaction.put(transaction.id(), transaction);
    }

    /**
     * Ensure that the transaction is indeed terminated.
     *
     * @param id
     */
    public void ensureTransactionTerminated(final TransactionId id) {
        final Transaction transaction = terminatedTransaction.remove(id);
        assertThat(transaction, not((Transaction)null));
        assertThat(transaction.state(), is(TransactionState.TERMINATED));
    }

    public static class Holder {

        private final SipMessage msg;
        private final Transaction transaction;

        public Holder(final Transaction transaction, final SipMessage msg) {
            this.transaction = transaction;
            this.msg = msg;
        }

        public SipMessage message() {
            return msg;
        }

        public Transaction transaction() {
            return transaction;
        }

    }
}
