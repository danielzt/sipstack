package io.sipstack.transaction.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.Transport;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.config.FlowConfiguration;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.TcpConnection;
import io.sipstack.netty.codec.sip.UdpConnection;
import io.sipstack.transaction.Transaction;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowFuture;
import io.sipstack.transport.TransportLayer;
import io.sipstack.transport.event.FlowEvent;
import io.sipstack.transport.impl.DefaultFlowStorage;
import io.sipstack.transport.impl.FlowActor;
import io.sipstack.transport.impl.FlowFutureImpl;
import io.sipstack.transport.impl.FlowStorage;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockTransportLayer implements TransportLayer {

    private final AtomicReference<CountDownLatch> latch = new AtomicReference<>(new CountDownLatch(1));

    private final FlowStorage flowStorage;

    private SipAndTransactionStorage storage = new SipAndTransactionStorage<FlowEvent>(flowEvent -> {
        if (flowEvent.isSipFlowEvent()) {
            return flowEvent.toSipFlowEvent().message();
        }
        return null;
    });

    private final ChannelHandlerContext ctx;
    private ChannelOutboundHandler handler;
    private Clock clock;

    /**
     *
     * @param ctx
     * @param topOfPipeLine
     */
    public MockTransportLayer(final FlowStorage storage, final ChannelHandlerContext ctx, final Clock clock) {
        this.flowStorage = storage;
        this.ctx = ctx;
        this.clock = clock;
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
        return new MockFlowBuilder(flowStorage, ctx, handler, host, clock);
    }

    @Override
    public Flow.Builder createFlow(InetSocketAddress remoteHost) throws IllegalArgumentException {
        throw new RuntimeException("Have to implement this again...");
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
        private Optional<Transport> transport = Optional.empty();
        private final Clock clock;
        private FlowStorage flowStorage;

        private MockFlowBuilder(final FlowStorage flowStorage,
                                final ChannelHandlerContext ctx,
                                final ChannelOutboundHandler handler,
                                final String host,
                                final Clock clock) {
            this.flowStorage = flowStorage;
            this.ctx = ctx;
            this.handler = handler;
            this.host = host;
            this.clock = clock;
        }

        @Override
        public Flow.Builder withPort(final int port) {
            this.port = port;
            return this;
        }

        @Override
        public Flow.Builder withTransport(final Transport transport) {
            this.transport = Optional.ofNullable(transport);
            return this;
        }

        @Override
        public Flow.Builder withNetworkInterface(final String interfaceName) {
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
        public CompletableFuture<Flow> connect() throws IllegalArgumentException {
            final int port = this.port == -1 ? defaultPort() : this.port;

            final InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
            final InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 5060);

            // TODO: want to mock up the channel so that it
            // returns the correct values as well.
            final Channel channel = new MockChannel(ctx, handler, localAddress, remoteAddress);
            final Transport transport = this.transport.orElse(Transport.udp);

            final Connection connection = transport == Transport.udp ?
                    new UdpConnection(channel, (InetSocketAddress)channel.remoteAddress()):
                    new TcpConnection(channel, (InetSocketAddress)channel.remoteAddress());

            final FlowActor actor = flowStorage.ensureFlow(connection);

            // TODO: currently, these flows will never fail so no reason
            // to setup the  onCancelled and onFailure...
            final CompletableFuture<Flow> future = CompletableFuture.completedFuture(actor.flow());
            future.thenAccept(onSuccess);
            return future;
        }

        /**
         * The default port should probably be coming from somewhere else.
         * @return
         */
        private int defaultPort() {
            final Transport transport = this.transport.orElse(Transport.udp);
            if (transport == Transport.udp || transport == Transport.tcp) {
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
