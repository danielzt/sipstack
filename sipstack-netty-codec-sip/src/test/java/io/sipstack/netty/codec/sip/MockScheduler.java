package io.sipstack.netty.codec.sip;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.sipstack.netty.codec.sip.actor.Cancellable;
import io.sipstack.netty.codec.sip.actor.InternalScheduler;
import io.sipstack.netty.codec.sip.event.Event;
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
public class MockScheduler implements InternalScheduler {

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
    public void fire(final int index) throws Exception {
        final MockCancellable task = scheduledTasks.remove(index);
        task.handler.userEventTriggered(task.ctx, task.event);
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
        for (final MockCancellable c : scheduledTasks) {
            ++index;
            if (c.event.isSipTimerEvent() && c.event.toSipTimerEvent().timer() == timer) {
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
                .filter(c -> c.event.isSipTimerEvent() && c.event.toSipTimerEvent().timer() == timer)
                .findFirst();
    }

    @Override
    public Cancellable schedule(final ChannelHandlerContext ctx, final Event event, final Duration delay) {
        throw new RuntimeException("Not sure we should use this one anymore");
    }

    @Override
    public Cancellable schedule(ChannelInboundHandler handler, ChannelHandlerContext ctx, Event event, Duration delay) {
        final MockCancellable cancellable = new MockCancellable(handler, ctx, event, delay);
        scheduledTasks.add(cancellable);
        latch.countDown();
        return cancellable;
    }
}
