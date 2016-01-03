package io.sipstack.transaction.impl;


import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.pkts.packet.sip.address.SipURI;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.junit.Assert.*;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockChannel implements Channel {

    private final ChannelHandlerContext ctx;
    private final ChannelOutboundHandler handler;
    private final InetSocketAddress localAddress;
    private final InetSocketAddress remoteAddress;

    private boolean hasCloseBeenCalled;

    public MockChannel(final ChannelHandlerContext ctx, final ChannelOutboundHandler handler,
                       final InetSocketAddress localAddress, final InetSocketAddress remoteAddress) {
        this.ctx = ctx;
        this.handler = handler;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public boolean hasCloseBeenCalled() {
        return hasCloseBeenCalled;
    }

    @Override
    public ChannelId id() {
        return null;
    }

    @Override
    public EventLoop eventLoop() {
        return null;
    }

    @Override
    public Channel parent() {
        return null;
    }

    @Override
    public ChannelConfig config() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isRegistered() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public ChannelMetadata metadata() {
        return null;
    }

    @Override
    public SocketAddress localAddress() {
        return localAddress;
    }

    @Override
    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    @Override
    public ChannelFuture closeFuture() {
        return null;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public long bytesBeforeUnwritable() {
        return 0;
    }

    @Override
    public long bytesBeforeWritable() {
        return 0;
    }

    @Override
    public Unsafe unsafe() {
        return null;
    }

    @Override
    public ChannelPipeline pipeline() {
        return null;
    }

    @Override
    public ByteBufAllocator alloc() {
        return null;
    }

    @Override
    public ChannelPromise newPromise() {
        return null;
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return null;
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return null;
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return null;
    }

    @Override
    public ChannelPromise voidPromise() {
        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture disconnect() {
        return null;
    }

    @Override
    public ChannelFuture close() {
        hasCloseBeenCalled = true;
        return null;
    }

    @Override
    public ChannelFuture deregister() {
        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        hasCloseBeenCalled = true;
        return null;
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return null;
    }

    @Override
    public Channel read() {
        return null;
    }

    @Override
    public ChannelFuture write(final Object msg) {
        return internalWrite(msg, Mockito.mock(ChannelPromise.class));
    }

    @Override
    public ChannelFuture write(final Object msg, final ChannelPromise promise) {
        return internalWrite(msg, promise);
    }

    private ChannelFuture internalWrite(final Object msg, final ChannelPromise promise) {
        try {
            handler.write(ctx, msg, promise);
        } catch (final Exception e) {
            // todo: should call the exception handler in this case
            e.printStackTrace();
            fail("Exception from handler");
        }
        return null;
    }

    @Override
    public Channel flush() {
        return this;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return internalWrite(msg, promise);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return internalWrite(msg, Mockito.mock(ChannelPromise.class));
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return null;
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return false;
    }

    @Override
    public int compareTo(Channel o) {
        return 0;
    }
}
