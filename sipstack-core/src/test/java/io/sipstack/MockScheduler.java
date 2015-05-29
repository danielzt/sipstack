package io.sipstack;

import io.hektor.core.ActorRef;
import io.hektor.core.Cancellable;
import io.hektor.core.Scheduler;
import io.sipstack.event.SipTimerEvent;
import io.sipstack.timers.SipTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockScheduler implements Scheduler {

    private static final Logger logger = LoggerFactory.getLogger(MockScheduler.class);

    private List<MockCancellable> scheduledTasks = new CopyOnWriteArrayList<>();

    public final CountDownLatch latch;

    public MockScheduler(final CountDownLatch latch) {
        this.latch = latch;
    }

    /**
     * Fire the timer at the given index.
     *
     * @param index
     */
    public void fire(final int index) {
        final MockCancellable event = scheduledTasks.remove(index);
        event.receiver.tell(event.msg, event.sender);
    }

    /**
     * Since we are dealing with so many SIP Timers this is a convenience method for
     * finding the timer and have it fire.
     *
     * Note, if the timer cannot be found
     *
     * @param timer
     */
    public void fire(final SipTimer timer) {
        try {
            fire(find(timer));
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Unable to find timer \"" + timer + "\"");
        }
    }

    private int find(final SipTimer timer) {
        int index = -1;
        for (final MockCancellable c : scheduledTasks) {
            ++index;
            if (c.msg instanceof SipTimerEvent && ((SipTimerEvent)c.msg).timer() == timer) {
                break;
            }
        }
        return index;
    }

    public int countCurrentTasks() {
        return scheduledTasks.size();
    }

    /**
     * Check if a particular timer is scheduled and if so return that cancellable.
     *
     * @param timer
     * @return
     */
    public Optional<MockCancellable> isScheduled(final SipTimer timer) {
        return scheduledTasks.stream()
                .filter(c -> c.msg instanceof SipTimerEvent && ((SipTimerEvent)c.msg).timer() == timer)
                .findFirst();
    }

    @Override
    public Cancellable schedule(final Object msg, final ActorRef receiver, final ActorRef sender, final Duration delay) {
        if (msg instanceof SipTimerEvent) {
            final SipTimerEvent sipTimer = (SipTimerEvent)msg;
            logger.info("SIP Timer \"{}\" was scheduled", sipTimer.timer());
        }

        final MockCancellable cancellable = new MockCancellable(msg, receiver, sender, delay);
        scheduledTasks.add(cancellable);
        latch.countDown();
        return cancellable;
    }
}
