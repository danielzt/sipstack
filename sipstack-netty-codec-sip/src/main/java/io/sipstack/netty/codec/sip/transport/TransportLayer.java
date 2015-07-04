package io.sipstack.netty.codec.sip.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.sipstack.netty.codec.sip.InboundOutboundHandlerAdapter;
import io.sipstack.netty.codec.sip.config.TransportLayerConfiguration;

import java.net.SocketAddress;

/**
 * @author jonas@jonasborjesson.com
 */
public class TransportLayer extends InboundOutboundHandlerAdapter {

    private final TransportLayerConfiguration config;

    public TransportLayer(final TransportLayerConfiguration config) {
        this.config = config;
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        System.err.println("Ok, so this is the TransportLayer read");
        ctx.read();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.err.println("Channel Read in transport layer " + msg);
        ctx.fireChannelRead(msg);
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
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
        // System.out.println("TransportLayer.write: " + msg);
        // final DatagramPacket pkt = new DatagramPacket(toByteBuf(msg), getRemoteAddress());
        ctx.write(msg, promise);
    }
}
