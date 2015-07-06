package io.sipstack.actor;

import io.netty.channel.ChannelHandlerContext;
import io.sipstack.event.Event;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.event.SipTimerEvent;
import io.sipstack.netty.codec.sip.transaction.TransactionLayer;
import io.sipstack.transaction.TransactionId;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * For many of our "actors", such as the {@link InviteServerTransaction}
 * it will only ever produce at most three things. Zero or one upstream event, zero or one
 * downstream event and zero or a few timer events. Therefore, this context only allows that
 * you do just that, i.e., only a single event of each kind.
 *
 * @author jonas@jonasborjesson.com
 */
public class SingleContext implements ActorContext, Scheduler {

    private Optional<Event> upstream = Optional.empty();

    private Optional<Event> downstream = Optional.empty();

    private ArrayList<SipTimer> timers = new ArrayList<>(3);

    private final InternalScheduler scheduler;

    private final TransactionId transactionId;

    private final Clock clock;

    private final TransactionLayer transactionLayer;

    private final ChannelHandlerContext handlerCtx;

    public SingleContext(final Clock clock,
                         final InternalScheduler scheduler,
                         final TransactionId transactionId,
                         final ChannelHandlerContext handlerCtx,
                         final TransactionLayer transactionLayer) {
        this.clock = clock;
        this.scheduler = scheduler;
        this.transactionId = transactionId;
        this.handlerCtx = handlerCtx;
        this.transactionLayer = transactionLayer;
    }

    public Scheduler scheduler() {
        return this;
    }

    @Override
    public void forwardUpstream(final Event event) {
        if (upstream.isPresent()) {
            throw new IllegalStateException("An upstream event has already been forwarded");
        }

        upstream = Optional.ofNullable(event);
    }

    @Override
    public void forwardDownstream(final Event event) {
        if (downstream.isPresent()) {
            throw new IllegalStateException("A downstream event has already been forwarded");
        }

        downstream = Optional.ofNullable(event);
    }

    public Optional<Event> upstream() {
        return upstream;
    }

    public Optional<Event> downstream() {
        return downstream;
    }

    @Override
    public Cancellable schedule(final SipTimer timer, final Duration delay) {

        if (transactionId == null) {
            // for stray responses etc there will be no transaction
            // available anymore and it really shouldn't be possible
            // for someone to create a timer in that case but if they
            // try then we will halt. Probably a better way to handle this
            // but we'll deal with that later.
            throw new RuntimeException("Unable to schedule a timer because there is no underlying transaction");
        }

        final SipTimerEvent event = SipTimerEvent
                .withTimer(timer)
                .withKey(transactionId)
                .build();

        // handlerCtx.executor().sc

        // throw new RuntimeException("Guess we got to come up with a new interface");
            // return scheduler.schedule(transactionLayer, handlerCtx, event, delay);

        // using the executor service associated with the handler itself
        // makes it better in that when the event is fired, it is being
        // executed on the correct threadpool. Otherwise it may have to
        // do a context switch.
        final ScheduledFuture<?> future = handlerCtx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    final TransactionId id = (TransactionId)event.key();
                    transactionLayer.userEventTriggered(handlerCtx, event);
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
