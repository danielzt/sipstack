package io.sipstack;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.sipstack.actor.Cancellable;
import io.sipstack.event.Event;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockCancellable implements Cancellable {
    public final Event event;
    public final ChannelHandlerContext ctx;
    public final Duration delay;
    public final ChannelInboundHandler handler;
    public final AtomicBoolean canceled = new AtomicBoolean(false);

    /**
     * Latch for keeping track of whether this cancellable has been
     * cancelled.
     */
    public final CountDownLatch cancelLatch = new CountDownLatch(1);

    public MockCancellable(final ChannelInboundHandler handler, final ChannelHandlerContext ctx, final Event event, final Duration delay) {
        this.handler = handler;
        this.ctx = ctx;
        this.event = event;
        this.delay = delay;
    }

    public boolean isCancelled() {
        return canceled.get();
    }

    @Override
    public boolean cancel() {
        canceled.getAndSet(true);
        cancelLatch.countDown();
        return true;
    }
}
