package io.sipstack.netty.codec.sip;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;

import java.net.SocketAddress;

/**
 * Combined inbound and outbound handler. When moving over to Netty 5, we should be able to
 * remove this one...
 *
 * @author jonas@jonasborjesson.com
 */
public class InboundOutboundHandlerAdapter implements ChannelInboundHandler, ChannelOutboundHandler {

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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("Channel read: " + msg);
        ctx.fireChannelRead(msg);
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Channel read compolete");
        ctx.fireChannelReadComplete();
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("user even triggered");
        ctx.fireUserEventTriggered(evt);
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelWritabilityChanged();
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("exception caught " + cause);
        ctx.fireExceptionCaught(cause);
    }

    /**
     * From ChannelHandler
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handler added");
    }

    /**
     * From ChannelHandler
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handler removed");
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
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        System.out.println("close");
        ctx.close(promise);
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        System.out.println("deregister");
        ctx.deregister(promise);
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        System.out.println("read");
        ctx.read();
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println("write " + msg);
        ctx.write(msg, promise);
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        System.out.println("flush it down!");
        ctx.flush();
    }
}
