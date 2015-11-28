package io.sipstack;

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
import io.pkts.packet.sip.SipMessage;
import io.sipstack.net.InboundOutboundHandlerAdapter;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.transport.event.FlowEvent;

import java.net.SocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockChannelHandlerContext implements ChannelHandlerContext {

    private AtomicReference<InboundOutboundHandlerAdapter> handler = new AtomicReference<>();

    public MockChannelHandlerContext(final InboundOutboundHandlerAdapter handler) {
        this.handler.set(handler);
    }

    /**
     * Latch keeping track of the number of times we have called "fireChannelRead"
     * By default the latch is set to 1 but you may want to change it if you
     * expect something else.
     */
    private AtomicReference<CountDownLatch> fireChannelReadLatch = new AtomicReference<>(new CountDownLatch(1));

    /**
     * List for keeping track of all the messages that has been pushed
     * through the context. These are the ones stored on the "fireChannelRead"
     */
    private List<Object> channelReadObjects = new CopyOnWriteArrayList<>();

    /**
     * Latch for keeping track of writes.
     */
    private AtomicReference<CountDownLatch> writeLatch = new AtomicReference<>(new CountDownLatch(1));

    /**
     * List for keeping track of all the messages that have been written to
     * this context.
     */
    private List<Object> writeObjects = new CopyOnWriteArrayList<>();

    public void reset() {
        reset(handler.get());
    }

    public void reset(final InboundOutboundHandlerAdapter adapter) {
        this.handler.set(adapter);

        channelReadObjects.clear();
        writeObjects.clear();
        fireChannelReadLatch.set(new CountDownLatch(1));
        writeLatch.set(new CountDownLatch(1));
    }

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
    public ChannelHandlerContext fireExceptionCaught(final Throwable cause) {
        return null;
    }

    @Override
    public ChannelHandlerContext fireUserEventTriggered(final Object event) {
        try {
            handler.get().userEventTriggered(this, event);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * Convenience method for making sure that nothing was written out to the context
     * and as such, we didn't actually try and send something across the socket.
     */
    public void assertNothingWritten() {
        assertThat(writeObjects.isEmpty(), is(true));
    }

    /**
     * Convenience method for making sure that nothing was forwarded in the
     * handler chain and as such the next one should not have gotten another
     * read event.
     */
    public void assertNothingRead() {
        assertThat(channelReadObjects.isEmpty(), is(true));
    }

    /**
     * When a channel handler processes a message (such as the invite server transaction)
     * it can choose to forward the message to the next handler in the pipeline. This
     * is a helper method to ensure that a particular message was indeed forwarded.
     *
     * Note thought that quite often you may just want to know that a particular SIP
     * message was forwarded but since a sip message itself is most of the times
     * wrapped in some other event you will not find it through this method. Therefore,
     * you may consider using the {@link MockChannelHandlerContext#assertSipMessageForwarded(SipMessage)}
     * in that case.
     *
     * @param msg
     */
    public void assertMessageForwarded(final Object msg) {
        assertThat(channelReadObjects.stream().filter(o -> o.equals(msg)).findFirst().isPresent(), is(true));
    }

    /**
     * Since most of the times a SIP message is wrapped in some other type of event
     * and as such, in order to find that particular sip message you have to find
     * the wrapping event, unwrap it and then compare them. Well, this method does
     * that for you...
     *
     * @param msg
     */
    public void assertSipMessageForwarded(final SipMessage msg) {
        final Optional<SipMessage> forwarded = findForwardedSipMessage(msg);
        assertThat("No such SIP message have been forwarded up the chain", forwarded.isPresent(), is(true));
    }

    /**
     * Sometimes you want to ensure that a particular message was NOT forwarded
     * @param msg
     */
    public void assertSipMessageNotForwarded(final SipMessage msg) {
        final Optional<SipMessage> forwarded = findForwardedSipMessage(msg);
        assertThat("Seems like the SIP message was forwarded after all", forwarded.isPresent(), is(false));
    }

    /**
     * Convenience method for locating a sip message that has been pushed upstream
     * in the event pipeline.
     *
     * @param msg
     * @return
     */
    public Optional<SipMessage> findForwardedSipMessage(final SipMessage msg) {
        return channelReadObjects.stream().filter(o -> msg.equals(unwrapEvent(o))).map(e -> unwrapEvent(e)).findFirst();
    }

    /**
     * Try and unwrap the event in order to find the SipMessage within. If
     * the object isn't a sip message event of some sort then null will be
     * returned.
     *
     * @param o
     * @return
     */
    public SipMessage unwrapEvent(final Object o) {
        if (o instanceof IOEvent) {
            final IOEvent ioEvent = (IOEvent)o;
            if (ioEvent.isSipMessageIOEvent()) {
                return ioEvent.toSipMessageIOEvent().message();
            }
        } else if (o instanceof FlowEvent) {
            final FlowEvent flowEvent = (FlowEvent) o;
            if (flowEvent.isSipFlowEvent()) {
                return flowEvent.toSipFlowEvent().message();
            }
        }

        return null;
    }

    /**
     * Find a message that was supposed to have been forwarded based on the type of the
     * message. Typically, you probably want to just use {@link #assertMessageForwarded(Object)}
     * but it is using equals to find the object and sometimes you may not have the full
     * object since it was an internally generated event and if you re-create the raw event
     * then we are breaking encapsulation.
     *
     * @param msg
     * @param <T>
     * @return
     */
    public <T> T findForwardedMessageByType(final Class<T> clazz) {
        return findMessageByType(clazz, channelReadObjects);
    }

    public void assertMessageWritten(final Object msg) {
        assertThat(writeObjects.stream().filter(o -> o.equals(msg)).findFirst().isPresent(),
                is(true));
    }

    public <T> T findWrittenMessageByType(final Class<T> clazz) {
        return findMessageByType(clazz, writeObjects);
    }

    private <T> T findMessageByType(final Class<T> clazz, final List<Object> list) {
        final Optional<Object> element = list.stream().filter(o -> clazz.isAssignableFrom(o.getClass())).findFirst();

        if (!element.isPresent()) {
            fail("No message of type \"" + clazz.getCanonicalName() + "\" has been forwareded");
        }

        return (T)element.get();
    }

    @Override
    public ChannelHandlerContext fireChannelRead(final Object msg) {
        channelReadObjects.add(msg);
        fireChannelReadLatch.get().countDown();
        return this;
    }

    public CountDownLatch fireChannelReadLatch() {
        return fireChannelReadLatch.get();
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

    @Override
    public ChannelFuture write(Object msg) {
        writeObjects.add(msg);
        writeLatch.get().countDown();

        // Currently we do not actually care about the
        // write futures but if we ever do we need
        // to return something useful.
        return null;
    }

    public CountDownLatch writeLatch() {
        return writeLatch.get();
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return write(msg);
    }

    @Override
    public ChannelHandlerContext flush() {
        return this;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        // We may want to keep track if we invoke flush as well
        // since there are performance penalties for doing that
        // too frequent. See excellent presentation here:
        // http://normanmaurer.me/presentations/2014-facebook-eng-netty/slides.html
        return write(msg);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return write(msg);
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
