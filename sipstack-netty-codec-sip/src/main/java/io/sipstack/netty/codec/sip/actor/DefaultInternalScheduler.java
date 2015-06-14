package io.sipstack.netty.codec.sip.actor;

import io.netty.channel.ChannelHandlerContext;
import io.sipstack.netty.codec.sip.event.Event;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of the {@link InternalScheduler} based off of a {@link ScheduledExecutorService}
 *
 * @author jonas@jonasborjesson.com
 */
public class DefaultInternalScheduler implements InternalScheduler {

    private final ScheduledExecutorService scheduler;

    public DefaultInternalScheduler(final ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Cancellable schedule(final ChannelHandlerContext ctx, final Event event, final Duration delay) {
        System.err.println("Scheduling new task");
        final ScheduledFuture<?> future = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                System.err.println("Fireing event...");
                ctx.fireUserEventTriggered(event);
            }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);

        return new CancellableImpl(future);
    }

    private static class CancellableImpl implements Cancellable {

        private final ScheduledFuture<?> future;

        private CancellableImpl(final ScheduledFuture<?> future) {
            this.future = future;
        }

        @Override
        public boolean cancel() {
            return future.cancel(false);
        }
    }
}
