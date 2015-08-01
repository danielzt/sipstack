package io.sipstack.transport.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.core.SipStack;
import io.sipstack.net.InboundOutboundHandlerAdapter;
import io.sipstack.net.ListeningPoint;
import io.sipstack.net.NetworkLayer;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.netty.codec.sip.Transport;
import io.sipstack.netty.codec.sip.UdpConnection;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowFuture;
import io.sipstack.transport.TransportLayer;
import io.sipstack.transport.event.FlowEvent;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The {@link DefaultTransportLayer} is responsible for maintaining {@link Flow}s, which represents
 * the connection between two endpoints.
 *
 * This layer also acts as the bridge between Netty and the rest of the {@link SipStack} (may change)
 *
 * @author jonas@jonasborjesson.com
 */
public class DefaultTransportLayer extends InboundOutboundHandlerAdapter implements TransportLayer {

    private final TransportLayerConfiguration config;

    // TODO: make the initial size configurable
    private final Map<ConnectionId, Flow> flows = new ConcurrentHashMap<>(1024);

    /**
     * The {@link DefaultTransportLayer} is the only one that actually
     * cares about the underlying network since it is the only
     * one that actually will manage connections etc. The rest
     * of the stack will either only see flows or simply just
     * asks to send a message and this layer will figure out
     * where the message is actually supposed to go.
     */
    private NetworkLayer network;

    public DefaultTransportLayer(final TransportLayerConfiguration config) {
        this.config = config;
    }

    public void useNetworkLayer(final NetworkLayer network) {
        this.network = network;
    }

    @Override
    public void read(final ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
        // Whenever we are done with read we will flush
        // any potential messages that need to go out.
        // Hence, never ever call writeAndFlush from
        // any place in the handler chain
        ctx.flush();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        try {
            final IOEvent event = (IOEvent)msg;

            if (!event.isSipMessageIOEvent()) {
                System.err.println("Ok, don't handle any other event that sip right now");
                return;
            }

            final Connection connection = event.connection();
            final ConnectionId connectionId = connection.id();
            final Flow flow = flows.computeIfAbsent(connectionId, obj -> {
                System.err.println("Got a new flow going");
                return new DefaultFlow(connection);
            });

            final SipMessage sipMsg = event.toSipMessageIOEvent().message();
            final FlowEvent flowEvent = sipMsg.isRequest() ? FlowEvent.create(flow, sipMsg.toRequest()) :
                    FlowEvent.create(flow, sipMsg.toResponse());
            ctx.fireChannelRead(flowEvent);
        } catch (final ClassCastException e) {
            e.printStackTrace();;
        }
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
        System.err.println("Channel registered: " + ctx.channel());
        ctx.fireChannelRegistered();
    }
    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        System.err.println("Channel un-registered " + ctx.channel());
        ctx.fireChannelUnregistered();
    }
    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.err.println("Channel active " + ctx.channel());
        ctx.fireChannelActive();
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.err.println("Channel in-active " + ctx.channel());
        ctx.fireChannelInactive();
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        System.err.println("Channel writability changed");
        ctx.fireChannelWritabilityChanged();
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        System.err.println("bidning to socket: " + localAddress);
        ctx.bind(localAddress, promise);
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        System.err.println("connecting to " + remoteAddress + " from local " + localAddress);
        ctx.connect(remoteAddress, localAddress, promise);
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        System.err.println("disconnecting...");
        ctx.disconnect(promise);
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        final FlowEvent event = (FlowEvent)msg;
        if (event.isSipFlowEvent()) {
            final SipMessage sip = event.toSipFlowEvent().message();

            // TODO: what if the flow doesn't have a connection present, such as in
            // the case of faulty flows (broken etc)? We probably should generate a
            // transport failed event of some sort.
            final InternalFlow flow = (InternalFlow)event.flow();
            flow.connection().ifPresent(c -> ctx.write(IOEvent.create(c, sip), promise));
        }
    }

    @Override
    public Flow.Builder createFlow(final String host) {
        PreConditions.ensureNotEmpty("You must specify the host to connect to", host);
        return new FlowBuilder(host);
    }

    private class FlowBuilder implements Flow.Builder {

        private Consumer<Flow> onSuccess;
        private Consumer<Flow> onFailure;
        private Consumer<Flow> onCancelled;
        private String host;
        private int port;
        private Transport transport;

        private FlowBuilder(final String host) {
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

            // TODO: need to ensure that the host is an actual IP-address
            final Transport transportToUse = transport == null ? Transport.udp : transport;
            final InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
            final Optional<ListeningPoint> lpToUse = network.getListeningPoint(transportToUse);

            // if we cannot find an appropriate ListeningPoint then we are dead. Return
            // a failed future.
            if (!lpToUse.isPresent()) {
                // TODO: create a failed FlowFuture and complete it right away. For now, exception...
                // TODO: or should we re-throw exception? I think we should stick with one thing so lets
                // go for a failed future.
                throw new RuntimeException("Guess we don't support the transport or something. Create failed FlowFuture here instead");
            }

            final ListeningPoint lp = lpToUse.get();
            final ConnectionId connectionId =
                    ConnectionId.create(transportToUse, lp.getLocalAddress(), remoteAddress);
            final Flow flow = flows.get(connectionId);
            FlowFuture flowFuture = null;
            if (flow != null) {
                flowFuture = new SuccessfulFlowFuture();
                invokeCallbackDirectly(flow, onSuccess);
            } else if (connectionId.isReliableTransport()) {
                // TODO: connect directly via the ListeningPoint instead.
                // TODO: actually need to store the future...
                final ChannelFuture future = network.connect(remoteAddress, transportToUse);
                final FlowFutureImpl flowFutureImpl = new FlowFutureImpl(flows, future, onSuccess, onFailure, onCancelled);
                future.addListener(flowFutureImpl);
                flowFuture = flowFutureImpl;
            } else {
                // for unreliable transports, we will simply create a connection as is without
                // actually "connecting".
                final Connection connection = new UdpConnection(lp.getChannel(), remoteAddress);
                final Flow newFlow = flows.computeIfAbsent(connection.id(), obj -> {
                    System.err.println("Created a new fake connection and returning it as a flow. This should only happen once");
                    return new DefaultFlow(connection);
                });
                flowFuture = new SuccessfulFlowFuture();
                invokeCallbackDirectly(newFlow, onSuccess);
            }
            return flowFuture;
        }

        private void invokeCallbackDirectly(final Flow flow, final Consumer<Flow> callback) {
            try {
                callback.accept(flow);
            } catch (final Throwable t) {
                // TODO: do something about this... guess we should propagate the exception up the chain or something
                t.printStackTrace();
            }
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
