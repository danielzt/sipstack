package io.sipstack;

import io.sipstack.actor.Cancellable;
import io.sipstack.core.SipTimerListener;
import io.sipstack.event.SipTimerEvent;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockCancellable implements Cancellable {
    public final SipTimerListener listener;
    public final SipTimerEvent event;
    public final Duration delay;
    public final AtomicBoolean canceled = new AtomicBoolean(false);

    /**
     * Latch for keeping track of whether this cancellable has been
     * cancelled.
     */
    public final CountDownLatch cancelLatch = new CountDownLatch(1);

    public MockCancellable(final SipTimerListener listener, final SipTimerEvent event, final Duration delay) {
        this.listener = listener;
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

