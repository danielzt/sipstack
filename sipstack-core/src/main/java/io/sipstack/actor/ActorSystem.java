/**
 * 
 */
package io.sipstack.actor;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.actor.ActorContext.DefaultActorContext;
import io.sipstack.actor.ActorSystem.DefaultActorSystem.DispatchJob;
import io.sipstack.config.SipConfiguration;
import io.sipstack.event.Event;
import io.sipstack.event.IOReadEvent;
import io.sipstack.netty.codec.sip.SipMessageEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ActorSystem {

    // void scheduleEvent(final Direction direction, final Duration delay, final Event event, final
    // PipeLine pipeLine);

    // void dispatchInboundEvent(final Event event);

    // void dispatchEvent(final Direction direction, final Event event, final PipeLine pipeLine);

    Timeout scheduleJob(final Duration delay, DispatchJob job);

    void dispatchJob(DispatchJob job);

    DispatchJob createJob(final Direction direction, final Event event, final PipeLine pipeLine);

    DispatchJob createJob(Direction direction, Event event);

    void receive(final SipMessageEvent event);

    static Builder withName(final String name) {
        PreConditions.ensureNotEmpty(name, "Name of Actor System cannot be null or empty");
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private final List<WorkerContext> workerContexts = new ArrayList<>();
        private SipConfiguration config;
        private Timer timer;

        private Builder(final String name) {
            this.name = name;
        }

        public Builder withTimer(final Timer timer) {
            this.timer = timer;
            return this;
        }

        public Builder withConfiguration(final SipConfiguration config) {
            this.config = config;
            return this;
        }

        public Builder withWorkerContext(final WorkerContext ctx) {
            PreConditions.ensureNotNull(ctx);
            this.workerContexts.add(ctx);
            return this;
        }

        public ActorSystem build() {
            PreConditions
            .ensureArgument(!this.workerContexts.isEmpty(), "You must specify at least one worker context");
            final Timer t = this.timer != null ? this.timer : new HashedWheelTimer();
            final SipConfiguration c = this.config != null ? this.config : new SipConfiguration();
            return new DefaultActorSystem(this.name, t, c, this.workerContexts);
        }
    }

    static class DefaultActorSystem implements ActorSystem {
        private final String name;

        private final ExecutorService workers;
        private final WorkerContext[] workerCtxs;
        private final int workerPoolSize;

        private final SipConfiguration config;

        private final Timer timer;

        /**
         * 
         */
        private DefaultActorSystem(final String name, final Timer timer, final SipConfiguration config,
                final List<WorkerContext> workerContexts) {
            this.timer = timer;
            this.name = name;
            this.config = config;

            this.workerPoolSize = workerContexts.size();
            this.workerCtxs = workerContexts.toArray(new WorkerContext[this.workerPoolSize]);
            this.workers = Executors.newFixedThreadPool(this.workerPoolSize);

            for (int i = 0; i < this.workerPoolSize; ++i) {
                final WorkerContext ctx = workerContexts.get(i);
                final Worker worker = new Worker(i, ctx.workerQueue());
                this.workers.execute(worker);
            }

        }

        /*
         * @Override public void scheduleEvent(final Direction direction, final Duration delay,
         * final Event event, final PipeLine pipeLine) { final TimerTask task = new TimerTask() {
         * 
         * @Override public void run(final Timeout timeout) throws Exception {
         * dispatchEvent(direction, event, pipeLine); } };
         * 
         * final Timeout timeout = this.timer.newTimeout(task, delay.toMillis(),
         * TimeUnit.MILLISECONDS); }
         */

        @Override
        public Timeout scheduleJob(final Duration delay, final DispatchJob job) {
            final TimerTask task = new TimerTask() {
                @Override
                public void run(final Timeout timeout) throws Exception {
                    dispatchJob(job);
                }
            };

            return this.timer.newTimeout(task, delay.toMillis(), TimeUnit.MILLISECONDS);
        }

        /**
         * Dispatch an inbound event using the pre-configured inbound pipe factory.
         * 
         * 
         * @param event
         */
        // @Override
        // public void dispatchInboundEvent(final Event event) {
        // dispatchJob(createJob(event));
        // }

        @Override
        public DispatchJob createJob(final Direction direction, final Event event) {
            final Key key = event.key();
            final int worker = Math.abs(key.hashCode() % this.workerPoolSize);
            final WorkerContext ctx = this.workerCtxs[worker];
            final PipeLineFactory factory = ctx.defaultPipeLineFactory();
            final PipeLine pipeLine = factory.newPipeLine();
            return new DispatchJob(this, worker, pipeLine, event, direction);
        }

        @Override
        public DispatchJob createJob(final Direction direction, final Event event, final PipeLine pipeLine) {
            final Key key = event.key();
            final int worker = Math.abs(key.hashCode() % this.workerPoolSize);
            return new DispatchJob(this, worker, pipeLine, event, direction);
        }


        @Override
        public void dispatchJob(final DispatchJob job) {
            final WorkerContext ctx = this.workerCtxs[job.getWorker()];
            if (!ctx.workerQueue().offer(job)) {
                // TODO: handle non accepted job
            }

        }

        @Override
        public void receive(final SipMessageEvent event) {
            final IOReadEvent<SipMessage> readEvent = IOReadEvent.create(event);
            final DispatchJob job = createJob(Direction.UPSTREAM, readEvent);
            dispatchJob(job);
        }

        public static final class DispatchJob implements Runnable {
            private final PipeLine pipeLine;
            private final Event event;
            private final int worker;
            private final ActorSystem system;
            private final Direction direction;

            public DispatchJob(final ActorSystem system, final int worker, final PipeLine pipeLine, final Event event,
                    final Direction direction) {
                this.direction = direction;
                this.system = system;
                this.worker = worker;
                this.pipeLine = pipeLine;
                this.event = event;
            }

            public int getWorker() {
                return this.worker;
            }

            public Event getEvent() {
                return this.event;
            }

            public Direction getDirection() {
                return this.direction;
            }

            @Override
            public void run() {
                if (this.direction == Direction.UPSTREAM) {
                    final DefaultActorContext ctx =
                            (DefaultActorContext) ActorContext.withInboundPipeLine(this.system, this.pipeLine);
                    ctx.forwardUpstreamEvent(this.event);
                } else {
                    final DefaultActorContext ctx =
                            (DefaultActorContext) ActorContext.withOutboundPipeLine(this.system, this.pipeLine);
                    ctx.forwardDownstreamEvent(this.event);
                }
            }
        }

    }

}
