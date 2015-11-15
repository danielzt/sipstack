package io.sipstack.actor;

import io.netty.channel.ChannelHandlerContext;
import io.sipstack.core.SipTimerListener;
import io.sipstack.event.SipTimerEvent;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.impl.DefaultTransactionLayer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

/**
 * For many of our "actors", such as the {@link InviteServerTransaction}
 * it will only ever produce at most three things. Zero or one upstream event, zero or one
 * downstream event and zero or a few timer events. Therefore, this context only allows that
 * you do just that, i.e., only a single event of each kind.
 *
 * @author jonas@jonasborjesson.com
 */
public class GenericSingleContext<T> implements ActorContext<T>, Scheduler {

    private Optional<T> upstream = Optional.empty();

    private Optional<T> downstream = Optional.empty();

    private Optional<T> forward = Optional.empty();

    private ArrayList<SipTimer> timers = new ArrayList<>(3);

    private final InternalScheduler scheduler;

    private final Clock clock;

    private final ChannelHandlerContext ctx;

    private final Object key;

    private final SipTimerListener timerListener;

    /**
     *
     * @param clock
     * @param ctx
     * @param scheduler
     * @param key the key used as part of timer. It has handed back to the {@link SipTimerListener}
     *            so it has a chance of figuring out to which internal (typically) actor is supposed
     *            to handle the timer event.
     * @param timerListener
     */
    public GenericSingleContext(final Clock clock,
                                final ChannelHandlerContext ctx,
                                final InternalScheduler scheduler,
                                final Object key,
                                final SipTimerListener timerListener) {
        this.clock = clock;
        this.ctx = ctx;
        this.key = key;
        this.scheduler = scheduler;
        this.timerListener = timerListener;
    }

    public Scheduler scheduler() {
        return this;
    }

    @Override
    public void forward(final T event) {
        if (forward.isPresent()) {
            throw new IllegalStateException("We have already forwarded an event");
        }

        forward = Optional.ofNullable(event);
    }

    @Override
    public void forwardUpstream(final T event) {
        if (upstream.isPresent()) {
            throw new IllegalStateException("An upstream event has already been forwarded");
        }

        upstream = Optional.ofNullable(event);
    }

    @Override
    public void forwardDownstream(final T event) {
        if (downstream.isPresent()) {
            throw new IllegalStateException("A downstream event has already been forwarded");
        }

        downstream = Optional.ofNullable(event);
    }

    public Optional<T> upstream() {
        return upstream;
    }

    public Optional<T> downstream() {
        return downstream;
    }

    public Optional<T> forward() {
        return forward;
    }

    @Override
    public Cancellable schedule(final SipTimer timer, final Duration delay) {

        if (key == null) {
            // For stray responses etc there will be no transaction
            // available anymore and it really shouldn't be possible
            // for someone to create a timer in that case but if they
            // try then we will halt. Probably a better way to handle this
            // but we'll deal with that later.
            throw new RuntimeException("Unable to schedule a timer because there is no underlying transaction");
        }

        final SipTimerEvent event = SipTimerEvent.withTimer(timer).withKey(key).withContext(ctx).build();
        return scheduler.schedule(timerListener, event, delay);

        /*
        return scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                transactionLayer.processSipTimerEvent(event);
            }
        }, delay);
        */

        // only temp until the scheduler is working again
        // return new CancellableImpl(null);
        // handlerCtx.executor().sc

        // throw new RuntimeException("Guess we got to come up with a new interface");
            // return scheduler.schedule(transactionLayer, handlerCtx, event, delay);

        // using the executor service associated with the handler itself
        // makes it better in that when the event is fired, it is being
        // executed on the correct threadpool. Otherwise it may have to
        // do a context switch.
        /*
        final ScheduledFuture<?> future = handlerCtx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    final TransactionId id = (TransactionId)event.key();
                    // transactionLayer.userEventTriggered(handlerCtx, event);
                } catch (final Throwable t) {
                    try {
                        // TODO:
                        // transactionLayer.exceptionCaught(handlerCtx, t);
                    } catch (Exception e) {
                        // i f@#$ give up.
                        e.printStackTrace();
                    }
                }
                // handlerCtx.fireUserEventTriggered(event);
            }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);

        return new CancellableImpl(future);
        */
    }

    private static class CancellableImpl implements Cancellable {

        private final ScheduledFuture<?> future;

        private CancellableImpl(final ScheduledFuture<?> future) {
            this.future = future;
        }

        @Override
        public boolean cancel() {
            // TODO: this is just temp until the scheduler is working again.
            if (future == null) {
                return false;
            }
            return future.cancel(false);
        }
    }
}
