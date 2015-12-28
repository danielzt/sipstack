package io.sipstack.actor;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.sipstack.core.SipTimerListener;
import io.sipstack.event.SipTimerEvent;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Simple wrapper around the netty HashWheelScheduler.
 *
 * @author jonas@jonasborjesson.com
 */
public class HashWheelScheduler implements InternalScheduler {

    private HashedWheelTimer timer;

    public HashWheelScheduler() {
        Executors.newCachedThreadPool();
        timer = new HashedWheelTimer(1, TimeUnit.SECONDS, 512);
    }



    @Override
    public Cancellable schedule(final Runnable job, final Duration delay) {
        final Task task = new Task(job);
        final Timeout timeout = timer.newTimeout(task, delay.toMillis(), TimeUnit.MILLISECONDS);
        return new CancellableTask(timeout);
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

        private final Runnable job;


        public Task(final Runnable job) {
            this.job = job;
        }

        @Override
        public void run(final Timeout timeout) throws Exception {
            job.run();
        }
    }
}
