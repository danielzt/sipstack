package io.sipstack.transaction.impl;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.event.SipRequestTransactionEvent;
import io.sipstack.transaction.event.TransactionEvent;
import io.sipstack.transport.event.FlowEvent;
import io.sipstack.transport.event.SipFlowEvent;

import java.net.SocketAddress;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Since everything within sipstack.io is really just a netty handler
 * they will all be invoked using the regular channelRead and write
 * methods, which all takes in a {@link ChannelHandlerContext}, which
 * this class is mocking.
 *
 * To make things easy when unit testing, this mock class also implements
 * application logic to simulate an application.
 *
 * @author jonas@jonasborjesson.com
 */
public class MockChannelHandlerContext implements ChannelHandlerContext {

    public MockChannelHandlerContext() {
        reset();
    }

    private SipAndTransactionStorage<FlowEvent> flowEventStorage = new SipAndTransactionStorage<>(flowEvent -> {
        if (flowEvent.isSipFlowEvent()) {
            return flowEvent.toSipFlowEvent().message();
        }
        return null;
    });

    private SipAndTransactionStorage<TransactionEvent> storage = new SipAndTransactionStorage<>(transactionEvent -> {
        if (transactionEvent.isSipTransactionEvent()) {
            return transactionEvent.toSipTransactionEvent().message();
        }
        return null;
    });

    public void ensureTransactionTerminated(final TransactionId id) {
        storage.ensureEvent("Transaction was not terminated", event ->
                event.isTransactionTerminatedEvent() && event.transaction().id().equals(id));
    }

    public SipRequestTransactionEvent assertAndConsumeRequest(final String method) {
        return storage.assertAndConsumeRequest(method).toSipRequestTransactionEvent();
    }

    public SipFlowEvent assertAndConsumeDownstreamRequest(final String method) {
        return flowEventStorage.assertAndConsumeRequest(method).toSipFlowEvent();
    }

    public Transaction assertAndConsumeResponse(final String method, final int responseStatus) {
        return storage.assertAndConsumeResponse(method, responseStatus).transaction();
    }

    public SipFlowEvent assertAndConsumeDownstreamResponse(final String method, final int responseStatus) {
        return flowEventStorage.assertAndConsumeResponse(method, responseStatus).toSipFlowEvent();
    }

    public void reset() {
        storage.reset();
        flowEventStorage.reset();
    }

    // Below this line are all the method exposed by the ChannelHandlerContext

    @Override
    public Channel channel() {
        return null;
    }

    @Override
    public EventExecutor executor() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public ChannelHandler handler() {
        return null;
    }

    @Override
    public boolean isRemoved() {
        return false;
    }

    @Override
    public ChannelHandlerContext fireChannelRegistered() {
        return null;
    }

    @Override
    public ChannelHandlerContext fireChannelUnregistered() {
        return null;
    }

    @Override
    public ChannelHandlerContext fireChannelActive() {
        return null;
    }

    @Override
    public ChannelHandlerContext fireChannelInactive() {
        return null;
    }

    @Override
    public ChannelHandlerContext fireExceptionCaught(Throwable cause) {
        return null;
    }

    @Override
    public ChannelHandlerContext fireUserEventTriggered(Object event) {
        return null;
    }

    /**
     * Whenever the {@link TransactionLayer} decides to forward a request
     * up the netty handler chain, it will call this method and since
     * the transaction layer only will produce {@link TransactionEvent}s
     * we will check that and then store away the event so that the
     * unit test can check it later...
     *
     * @param msg
     * @return
     */
    @Override
    public ChannelHandlerContext fireChannelRead(Object msg) {
        assertThat(msg instanceof TransactionEvent, is(true));
        final TransactionEvent event = (TransactionEvent)msg;
        storage.store(event);
        return this;
    }

    @Override
    public ChannelHandlerContext fireChannelReadComplete() {
        return null;
    }

    @Override
    public ChannelHandlerContext fireChannelWritabilityChanged() {
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
        return null;
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelHandlerContext read() {
        return null;
    }

    /**
     * Remember that we are expecting a FlowEvent to be written since
     * the layer below the {@link io.sipstack.transaction.TransactionLayer} is expecting
     * FlowEvents to take place.
     *
     * @param msg
     * @return
     */
    @Override
    public ChannelFuture write(final Object msg) {
        assertThat(msg instanceof FlowEvent, is(true));
        final FlowEvent event = (FlowEvent)msg;
        flowEventStorage.store(event);
        return null;
    }

    @Override
    public ChannelFuture write(final Object msg, final ChannelPromise promise) {
        throw new RuntimeException("I wonder if we should always call this one instead...");
    }

    @Override
    public ChannelHandlerContext flush() {
        return null;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
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
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return null;
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return false;
    }
}
