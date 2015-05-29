package io.sipstack;

import io.hektor.core.ActorRef;
import io.hektor.core.Cancellable;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockCancellable implements Cancellable {
    public final Object msg;
    public final ActorRef receiver;
    public final ActorRef sender;
    public final Duration delay;
    public final AtomicBoolean canceled = new AtomicBoolean(false);

    /**
     * Latch for keeping track of whether this cancellable has been
     * cancelled.
     */
    public final CountDownLatch cancelLatch = new CountDownLatch(1);

    public MockCancellable(final Object msg, final ActorRef receiver, final ActorRef sender, final Duration delay) {
        this.msg = msg;
        this.receiver = receiver;
        this.sender = sender;
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

