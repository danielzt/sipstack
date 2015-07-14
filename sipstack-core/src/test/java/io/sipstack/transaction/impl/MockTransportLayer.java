package io.sipstack.transaction.impl;

import io.netty.channel.Channel;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.TcpConnection;
import io.sipstack.netty.codec.sip.Transport;
import io.sipstack.netty.codec.sip.UdpConnection;
import io.sipstack.transaction.Transaction;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowFuture;
import io.sipstack.transport.Transports;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.mockito.Mockito.when;

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
        return new MockFlowBuilder(host);
    }

    /**
     * TODO: This is stupid - this is a complete copy-paste from the real ones that
     * current exists within the {@link MockTransportLayer} but those should
     * rather be extracted out in a generic way so those also get tested...
     *
     */
    private static class MockFlowBuilder implements Flow.Builder {

        private Consumer<Flow> onSuccess;
        private Consumer<Flow> onFailure;
        private Consumer<Flow> onCancelled;
        private String host;
        private int port;
        private Transport transport;

        private MockFlowBuilder(final String host) {
            this.host = host;
        }

        @Override
        public Flow.Builder withPort(final int port) {
            this.port = port;
            return this;
        }

        @Override
        public Flow.Builder withTransport(final Transport transport) {
            this.transport = transport;
            return this;
        }

        @Override
        public Flow.Builder onSuccess(final Consumer<Flow> consumer) {
            this.onSuccess = consumer;
            return this;
        }

        @Override
        public Flow.Builder onFailure(final Consumer<Flow> consumer) {
            this.onFailure = consumer;
            return this;
        }

        @Override
        public Flow.Builder onCancelled(Consumer<Flow> consumer) {
            this.onCancelled = consumer;
            return this;
        }

        @Override
        public FlowFuture connect() throws IllegalArgumentException {
            final int port = this.port == -1 ? defaultPort() : this.port;

            final InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
            final InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 5060);

            // TODO: want to mock up the channel so that it
            // returns the correct values as well.
            final Channel channel = Mockito.mock(Channel.class);
            when(channel.localAddress()).thenReturn(localAddress);
            when(channel.remoteAddress()).thenReturn(remoteAddress);

            Connection connection = null;
            if (transport == null || transport == Transport.udp) {
                connection = new UdpConnection(channel, remoteAddress);
            } else {
                connection = new TcpConnection(channel, remoteAddress);
            }

            // final MockFuture<Connection> mockFuture = new MockFuture<>(connection);
            // final FlowFutureImpl flowFuture = new FlowFutureImpl(mockFuture, onSuccess, onFailure, onCancelled);
            // mockFuture.addListener(flowFuture);
            // return flowFuture;
            throw new RuntimeException("TODO again");
        }

        /**
         * The default port should probably be coming from somewhere else.
         * @return
         */
        private int defaultPort() {
            if (transport == null || transport == Transport.udp || transport == Transport.tcp) {
                return 5060;
            }

            if (transport == Transport.tls) {
                return 5061;
            }

            if (transport == Transport.ws) {
                return 5062;
            }

            if (transport == Transport.wss) {
                return 5063;
            }

            return 5060;
        }
    }
}
