package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.transport.Flow;
import io.sipstack.transport.Transports;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockTransportLayer implements Transports {

    /**
     * A list of all messages we have received and in real life
     * would have sent out across the network (well, supposedly anyway)
     */
    private List<SipMessage> messages = new ArrayList<>();

    private final AtomicReference<CountDownLatch> latch = new AtomicReference<>(new CountDownLatch(1));

    public void assertAndConsumeResponse(final String method, final int responseStatus) {

        synchronized (messages) {
            final Iterator<SipMessage> it = messages.iterator();
            int count = 0;
            while (it.hasNext()) {
                final SipMessage msg = it.next();
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
        }

    }

    public void reset() {
        reset(1);
    }

    public void reset(final int countdownLatchCount) {
        synchronized (messages) {
            messages.clear();
        }
        latch.set(new CountDownLatch(countdownLatchCount));
    }

    public CountDownLatch latch() {
        return latch.get();
    }

    @Override
    public void write(final SipMessage msg) {
        synchronized (messages) {
            messages.add(msg);
        }
        latch.get().countDown();
    }

    @Override
    public void write(final Flow flow, final SipMessage msg) {
        synchronized (messages) {
            messages.add(msg);
        }

        latch.get().countDown();
    }
}
