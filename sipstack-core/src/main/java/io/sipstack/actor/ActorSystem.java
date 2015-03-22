/**
 * 
 */
package io.sipstack.actor;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.actor.ActorContext.DefaultActorContext;
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
public class ActorSystem {

    private final String name;

    private final ExecutorService workers;
    private final WorkerContext[] workerCtxs;
    private final int workerPoolSize;

    // private final ApplicationSupervisor[] applicationSupervisors = new
    // ApplicationSupervisor[this.workerPoolSize];
    // private final TransactionSupervisor[] transactionSupervisors = new
    // TransactionSupervisor[this.workerPoolSize];
    // private final TransportSupervisor[] transpotSupervisors = new
    // TransportSupervisor[this.workerPoolSize];
    // private final PipeLineFactory[] inboundPipefactory = new
    // PipeLineFactory[this.workerPoolSize];

    private final SipConfiguration config;

    private final HashedWheelTimer timer;

    public static Builder withName(final String name) {
        PreConditions.ensureNotEmpty(name, "Name of Actor System cannot be null or empty");
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private final List<WorkerContext> workerContexts = new ArrayList<>();
        private SipConfiguration config;

        private Builder(final String name) {
            this.name = name;
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
            final SipConfiguration c = this.config != null ? this.config : new SipConfiguration();
            return new ActorSystem(this.name, c, this.workerContexts);
        }

    }

    /**
     * 
     */
    private ActorSystem(final String name, final SipConfiguration config, final List<WorkerContext> workerContexts) {
        this.name = name;
        this.config = config;

        this.timer = new HashedWheelTimer();

        this.workerPoolSize = workerContexts.size();
        this.workerCtxs = workerContexts.toArray(new WorkerContext[this.workerPoolSize]);
        this.workers = Executors.newFixedThreadPool(this.workerPoolSize);

        for (int i = 0; i < this.workerPoolSize; ++i) {
            final WorkerContext ctx = workerContexts.get(i);
            final Worker worker = new Worker(i, ctx.workerQueue());
            this.workers.execute(worker);
        }

    }

    public void scheduleEvent(final Duration delay, final Event event, final PipeLine pipeLine) {
        final TimerTask task = new TimerTask() {
            @Override
            public void run(final Timeout timeout) throws Exception {
                dispatchEvent(event, pipeLine);
            }
        };

        final Timeout timeout = this.timer.newTimeout(task, delay.toMillis(), TimeUnit.MILLISECONDS);
        timeout.isCancelled();
    }

    /**
     * Dispatch an inbound event using the pre-configured inbound pipe factory.
     * 
     * @param event
     */
    public void dispatchInboundEvent(final Event event) {
        final Key key = event.key();
        final int worker = Math.abs(key.hashCode() % this.workerPoolSize);
        final WorkerContext ctx = this.workerCtxs[worker];
        final PipeLineFactory factory = ctx.defaultPipeLineFactory();
        final PipeLine pipeLine = factory.newPipeLine();
        final DispatchJob job = new DispatchJob(this, worker, pipeLine, event);
        if (!ctx.workerQueue().offer(job)) {
            // TODO: handle non accepted job
        }
    }

    /**
     * Dispatch an event using the supplied {@link PipeLine}. Since the pipe line is already
     * supplied there is no "inbound" vs "outbound" because the direction is determined by the
     * {@link PipeLine} itself.
     * 
     * @param event
     * @param pipeLine
     */
    public void dispatchEvent(final Event event, final PipeLine pipeLine) {
        final Key key = event.key();
        final int worker = Math.abs(key.hashCode() % this.workerPoolSize);
        final WorkerContext ctx = this.workerCtxs[worker];
        final DispatchJob job = new DispatchJob(this, worker, pipeLine, event);
        if (!ctx.workerQueue().offer(job)) {
            // TODO: handle non accepted job
        }
    }

    public void receive(final SipMessageEvent event) {
        final IOReadEvent<SipMessage> readEvent = IOReadEvent.create(event);
        dispatchInboundEvent(readEvent);
    }

    private static final class DispatchJob implements Runnable {
        private final PipeLine pipeLine;
        private final Event event;
        private final int worker;
        private final ActorSystem system;

        public DispatchJob(final ActorSystem system, final int worker, final PipeLine pipeLine, final Event event) {
            this.system = system;
            this.worker = worker;
            this.pipeLine = pipeLine;
            this.event = event;
        }

        @Override
        public void run() {
            final DefaultActorContext ctx =
                    (DefaultActorContext) ActorContext.withInboundPipeLine(this.system, this.pipeLine);
            ctx.forwardUpstreamEvent(this.event);
        }
    }

}
