package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Helper class for unit tests that needs to consume sip messages and transactions.
 * It offer up storage as well as helper method for checking that a particular
 * message indeed was received etc.
 *
 * @author jonas@jonasborjesson.com
 */
public class SipAndTransactionStorage<T> {

    /**
     * A list of all messages we have received...
     */
    private List<T> messages = new ArrayList<>();

    private final Function<T, SipMessage> mapper;

    public SipAndTransactionStorage(final Function<T, SipMessage> mapper) {
        this.mapper = mapper;
    }

    public void reset() {
        synchronized (messages) {
            messages.clear();
        }
    }


    public Transaction assertTransaction(final SipMessage msg) {
        // return assertTransaction(TransactionId.create(msg));
        return null;
    }

    public Transaction assertTransaction(final TransactionId id) {
        // final Transaction t = allTransactions.get(id);
        // assertThat(t, not((Transaction) null));
        // return t;
        return null;
    }

    public void store(T event) {
        synchronized (messages) {
            messages.add(event);
        }
    }

    /**
     * Ensure that the message was received/sent but do NOT consume it.
     *
     * @param method
     * @return
     */
    public T assertRequest(final String method) {
        return assertRequest(false, method);
    }

    public T consumeRequest(final SipRequest request) {
        return assertRequest(true, request.getMethod().toString());
    }

    /**
     * Make sure that there was a SINGLE request with the specified method received by
     * this transaction user.
     *
     * @param method
     */
    public T assertAndConsumeRequest(final String method) {
        return assertRequest(true, method);
    }

    private T assertRequest(final boolean consume, final String method) {
        synchronized (messages) {
            final Iterator<T> it = messages.iterator();
            int count = 0;
            T result = null;
            while (it.hasNext()) {
                final T event = it.next();
                final SipMessage msg = mapper.apply(event);
                if (msg != null && msg.isRequest()) {
                    final SipRequest request = msg.toRequest();
                    if (method.equalsIgnoreCase(request.getMethod().toString())) {
                        if (consume) {
                            it.remove();
                        }
                        result = event;
                        ++count;
                    }
                }
            }

            if (count == 0) {
                fail("Expected a " + method.toUpperCase() + " request but didn't find one");
            } else if (count > 1) {
                fail("Found many " + method.toUpperCase() + " requests");
            }

            return result;
        }
    }

    public T assertAndConsumeResponse(final String method, final int responseStatus) {

        synchronized (messages) {
            final Iterator<T> it = messages.iterator();
            int count = 0;
            T result = null;
            while (it.hasNext()) {
                final T event = it.next();
                final SipMessage msg = mapper.apply(event);
                if (msg != null && msg.isResponse()) {
                    final SipResponse response = msg.toResponse();
                    if ( method.equalsIgnoreCase(response.getMethod().toString())
                            && response.getStatus() == responseStatus) {
                        it.remove();
                        result = event;
                        ++count;
                    }
                }
            }

            if (count == 0) {
                fail("Expected a " + responseStatus + " response to " + method.toUpperCase() + " but didn't find one");
            } else if (count > 1) {
                fail("Found many " + responseStatus + " response to " + method.toUpperCase() + " but didn't find one");
            }

            return result;
        }
    }

    /**
     * Use a filter to find a particular event.
     *
     * @param predicate
     * @return
     */
    public T ensureEvent(String errorMessage, Predicate<T> predicate) {
        Optional<T> result = messages.stream().filter(predicate).findFirst();
        assertThat(errorMessage, result.isPresent(), is(true));
        return result.get();
    }

    public T ensureEvent(Predicate<T> predicate) {
        return ensureEvent("Unable to find the event you were looking for", predicate);
    }

}
