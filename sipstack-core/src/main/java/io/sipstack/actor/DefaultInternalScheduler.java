package io.sipstack.actor;

import io.sipstack.core.SipTimerListener;
import io.sipstack.event.SipTimerEvent;

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
    public Cancellable schedule(final Runnable job, final Duration delay) {
        final ScheduledFuture<?> future = scheduler.schedule(job, delay.toMillis(), TimeUnit.MILLISECONDS);
        return new CancellableImpl(future);
    }

    @Override
    public Cancellable schedule(final SipTimerListener listener, final SipTimerEvent timerEvent, final Duration delay) {
        return schedule(new Runnable() {
            @Override
            public void run() {
                listener.onTimeout(timerEvent);
            }
        }, delay);
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
