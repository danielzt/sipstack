package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.netty.codec.sip.Transport;
import io.sipstack.transaction.Transaction;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowFuture;
import io.sipstack.transport.Transports;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockTransportLayer implements Transports {

    private final AtomicReference<CountDownLatch> latch = new AtomicReference<>(new CountDownLatch(1));

    private SipAndTransactionStorage storage = new SipAndTransactionStorage();

    public void reset() {
        reset(1);
    }

    public void assertAndConsumeResponse(final String method, final int responseStatus) {
        storage.assertAndConsumeResponse(method, responseStatus);
    }

    public Transaction assertAndConsumeRequest(final String method) {
        return storage.assertAndConsumeRequest(method);
    }

    public SipRequest assertRequest(final String method) {
        return storage.assertRequest(method);
    }

    public void consumeRequest(final SipRequest request) {
        storage.consumeRequest(request);
    }

    public void reset(final int countdownLatchCount) {
        storage.reset();
        latch.set(new CountDownLatch(countdownLatchCount));
    }

    public CountDownLatch latch() {
        return latch.get();
    }

    @Override
    public void write(final SipMessage msg) {
        storage.store(msg);
        latch.get().countDown();
    }

    @Override
    public void write(final Flow flow, final SipMessage msg) {
        storage.store(msg);
        latch.get().countDown();
    }

    @Override
    public Flow.Builder createFlow(final String host) throws IllegalArgumentException {
        PreConditions.ensureNotEmpty(host, "Host cannot be empty");
        return null;
    }

    private static final class MockFlowBuilder implements Flow.Builder {

        private final String host;

        private MockFlowBuilder(final String host) {
            this.host = host;
        }

        @Override
        public Flow.Builder withPort(int port) {
            return null;
        }

        @Override
        public Flow.Builder withTransport(Transport transport) {
            return null;
        }

        @Override
        public Flow.Builder onSuccess(Consumer<Flow> consumer) {
            return null;
        }

        @Override
        public Flow.Builder onFailure(Consumer<Flow> consumer) {
            return null;
        }

        @Override
        public Flow.Builder onCancelled(Consumer<Flow> consumer) {
            return null;
        }

        @Override
        public FlowFuture connect() throws IllegalArgumentException {
            return null;
        }
    }

    private static final class MockFlowFuture implements FlowFuture {

        @Override
        public boolean cancel() {
            return false;
        }
    }
}
