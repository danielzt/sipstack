package io.sipstack;

import io.sipstack.actor.Cancellable;
import io.sipstack.actor.InternalScheduler;
import io.sipstack.core.SipTimerListener;
import io.sipstack.event.SipTimerEvent;
import io.sipstack.netty.codec.sip.SipTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockScheduler implements InternalScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MockScheduler.class);

    // private List<MockCancellable> scheduledTasks = new CopyOnWriteArrayList<>();
    private List<MockCancellable> scheduledTasks = new ArrayList<>();

    public final CountDownLatch latch;

    public MockScheduler(final CountDownLatch latch) {
        this.latch = latch;
    }

    public void reset() {
        synchronized (scheduledTasks) {
            scheduledTasks.clear();
        }
    }

    /**
     * Fire the timer at the given index.
     *
     * @param index
     */
    public void fire(final int index) throws Exception {
        MockCancellable task = null;
        synchronized (scheduledTasks) {
            task = scheduledTasks.remove(index);
        }
        task.listener.onTimeout(task.event);
    }

    /**
     * Since we are dealing with so many SIP Timers this is a convenience method for
     * finding the timer and have it fire.
     *
     * Note, if the timer cannot be found
     *
     * @param timer
     */
    public void fire(final SipTimer timer) throws Exception {
        try {
            fire(find(timer));
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Unable to find timer \"" + timer + "\"");
        }
    }

    private int find(final SipTimer timer) {
        int index = -1;
        synchronized (scheduledTasks) {
            for (final MockCancellable c : scheduledTasks) {
                ++index;
                if (c.event.isSipTimerEvent() && c.event.toSipTimerEvent().timer() == timer) {
                    break;
                }
            }
        }
        return index;
    }

    public int countCurrentTasks() {
        synchronized (scheduledTasks) {
            return scheduledTasks.size();
        }
    }

    /**
     * Check if a particular timer is scheduled and if so return that cancellable.
     *
     * @param timer
     * @return
     */
    public Optional<MockCancellable> isScheduled(final SipTimer timer) {
        synchronized (scheduledTasks) {
            return scheduledTasks.stream()
                    .filter(c -> c.event.isSipTimerEvent() && c.event.toSipTimerEvent().timer() == timer)
                    .findFirst();
        }
    }

    @Override
    public Cancellable schedule(final Runnable run, Duration delay) {
        throw new RuntimeException("Sorry, the mock hasn't implemented this one yet");
    }

    @Override
    public Cancellable schedule(SipTimerListener listener, SipTimerEvent timerEvent, Duration delay) {
        synchronized (scheduledTasks) {
            final MockCancellable cancellable = new MockCancellable(listener, timerEvent, delay);
            scheduledTasks.add(cancellable);
            latch.countDown();
            return cancellable;
        }
    }
}
