package io.sipstack.transaction.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.config.FlowConfiguration;
import io.sipstack.netty.codec.sip.Transport;
import io.sipstack.transaction.Transaction;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowFuture;
import io.sipstack.transport.TransportLayer;
import io.sipstack.transport.event.FlowEvent;
import io.sipstack.transport.impl.DefaultFlowStorage;
import io.sipstack.transport.impl.FlowFutureImpl;
import io.sipstack.transport.impl.FlowStorage;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockTransportLayer implements TransportLayer {

    private final AtomicReference<CountDownLatch> latch = new AtomicReference<>(new CountDownLatch(1));

    private SipAndTransactionStorage storage = new SipAndTransactionStorage<FlowEvent>(flowEvent -> {
        if (flowEvent.isSipFlowEvent()) {
            return flowEvent.toSipFlowEvent().message();
        }
        return null;
    });

    private final ChannelHandlerContext ctx;
    private ChannelOutboundHandler handler;

    /**
     *
     * @param ctx
     * @param topOfPipeLine
     */
    public MockTransportLayer(final ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public void setChannelOutboundHandler(final ChannelOutboundHandler handler) {
        this.handler = handler;
    }

    public void reset() {
        reset(1);
    }

    public void assertAndConsumeResponse(final String method, final int responseStatus) {
        storage.assertAndConsumeResponse(method, responseStatus);
    }

    public Transaction assertAndConsumeRequest(final String method) {
        // return storage.assertAndConsumeRequest(method).transaction();
        return null;
    }

    public SipRequest assertRequest(final String method) {
        return null;
        // return storage.assertRequest(method);
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
    public Flow.Builder createFlow(final String host) throws IllegalArgumentException {
        PreConditions.ensureNotEmpty(host, "Host cannot be empty");
        return new MockFlowBuilder(ctx, handler, host);
    }

    /**
     * TODO: This is stupid - this is a complete copy-paste from the real ones that
     * current exists within the {@link MockTransportLayer} but those should
     * rather be extracted out in a generic way so those also get tested...
     *
     */
    private static class MockFlowBuilder implements Flow.Builder {

        private final ChannelHandlerContext ctx;
        private final ChannelOutboundHandler handler;

        private Consumer<Flow> onSuccess;
        private Consumer<Flow> onFailure;
        private Consumer<Flow> onCancelled;
        private String host;
        private int port;
        private Transport transport;

        private MockFlowBuilder(final ChannelHandlerContext ctx, final ChannelOutboundHandler handler, final String host) {
            this.ctx = ctx;
            this.handler = handler;
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
            final Channel channel = new MockChannel(ctx, handler, localAddress, remoteAddress);

            // TODO: need to mock out the transport eventually as well.

            final MockChannelFuture mockFuture = new MockChannelFuture(channel);
            final FlowConfiguration flowConfiguration = new FlowConfiguration();
            final FlowStorage flowStorage = new DefaultFlowStorage(flowConfiguration);
            final FlowFutureImpl flowFuture = new FlowFutureImpl(flowStorage, mockFuture, onSuccess, onFailure, onCancelled);

            // this will cause the future to call the callback right away because
            // it is a completed future already.
            mockFuture.addListener(flowFuture);


            return flowFuture;
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
