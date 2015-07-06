package io.sipstack.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.core.SipStack;
import io.sipstack.net.InboundOutboundHandlerAdapter;
import io.sipstack.net.NetworkLayer;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.netty.codec.sip.SipMessageEvent;

import java.net.SocketAddress;
import java.util.Optional;

/**
 * The {@link TransportLayer} is responsible for maintaining {@link Flow}s, which represents
 * the connection between two endpoints.
 *
 * This layer also acts as the bridge between Netty and the rest of the {@link SipStack}
 *
 * @author jonas@jonasborjesson.com
 */
public class TransportLayer extends InboundOutboundHandlerAdapter implements Transports {

    private final TransportLayerConfiguration config;

    private final TransportUser transportUser;

    /**
     * The {@link TransportLayer} is the only one that actually
     * cares about the underlying network since it is the only
     * one that actually will manage connections etc. The rest
     * of the stack will either only see flows or simply just
     * asks to send a message and this layer will figure out
     * where the message is actually supposed to go.
     */
    private NetworkLayer network;

    public TransportLayer(final TransportLayerConfiguration config, final TransportUser transportUser) {
        this.config = config;
        this.transportUser = transportUser;
    }

    public void useNetworkLayer(final NetworkLayer network) {
        this.network = network;
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        try {
            final SipMessageEvent event = (SipMessageEvent)msg;
            final Connection connection = event.connection();
            Flow flow = null;
            final Optional<Object> optional = connection.fetchObject();
            if (optional.isPresent()) {
                flow = (Flow)optional.get();
            } else {
                flow = new DefaultFlow(connection, ctx);
                connection.storeObject(flow);
            }
            transportUser.onMessage(flow, ((SipMessageEvent) msg).message());
        } catch (final ClassCastException e) {
            e.printStackTrace();;
        }
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelRegistered();
    }
    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelUnregistered();

    }
    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
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
        ctx.connect(remoteAddress, localAddress, promise);
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.disconnect(promise);
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        // we should never end up here...
        ctx.write(msg, promise);
    }

    @Override
    public void write(final SipMessage msg) {
        throw new RuntimeException("TODO");
    }

    @Override
    public void write(final Flow flow, final SipMessage msg) {
        throw new RuntimeException("TODO");
    }


    private class DefaultFlow implements Flow {
        private final ChannelHandlerContext ctx;
        private final Connection connection;

        DefaultFlow(final Connection connection, final ChannelHandlerContext ctx) {
            this.connection = connection;
            this.ctx = ctx;
        }

        @Override
        public ConnectionId id() {
            return connection.id();
        }

    }
}
