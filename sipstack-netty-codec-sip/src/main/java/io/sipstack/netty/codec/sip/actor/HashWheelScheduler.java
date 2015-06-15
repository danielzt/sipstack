package io.sipstack.netty.codec.sip.actor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.sipstack.netty.codec.sip.event.Event;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Simple wrapper around the netty HashWheelScheduler.
 *
 * @author jonas@jonasborjesson.com
 */
public class HashWheelScheduler implements InternalScheduler {

    private HashedWheelTimer timer;

    public HashWheelScheduler() {
        timer = new HashedWheelTimer(1, TimeUnit.SECONDS, 512);
    }


    @Override
    public Cancellable schedule(ChannelHandlerContext ctx, Event event, Duration delay) {
        throw new RuntimeException("Dont use");
    }

    @Override
    public Cancellable schedule(final ChannelInboundHandler handler, final ChannelHandlerContext ctx, final Event event, final Duration delay) {
        final Task task = new Task(handler, ctx, event);
        final Timeout timeout = timer.newTimeout(task, delay.toMillis(), TimeUnit.MILLISECONDS);
        return new CancellableTask(timeout);
    }

    private static class CancellableTask implements Cancellable {
        private final Timeout timeout;

        public CancellableTask(final Timeout timeout) {
            this.timeout = timeout;
        }

        @Override
        public boolean cancel() {
            return timeout.cancel();
        }
    }

    private static class Task implements TimerTask {

        private final Event event;
        private final ChannelInboundHandler handler;
        private final ChannelHandlerContext ctx;


        public Task(final ChannelInboundHandler handler, final ChannelHandlerContext ctx, final Event event) {
            this.event = event;
            this.handler = handler;
            this.ctx = ctx;
        }

        @Override
        public void run(final Timeout timeout) throws Exception {
            handler.userEventTriggered(ctx, event);
        }
    }
}
