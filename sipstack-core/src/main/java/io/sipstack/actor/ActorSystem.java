/**
 * 
 */
package io.sipstack.actor;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.actor.ActorContext.DefaultActorContext;
import io.sipstack.application.ApplicationSupervisor;
import io.sipstack.event.Event;
import io.sipstack.event.IOReadEvent;
import io.sipstack.netty.codec.sip.SipMessageEvent;
import io.sipstack.transaction.impl.TransactionSupervisor;
import io.sipstack.transport.TransportSupervisor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author jonas@jonasborjesson.com
 */
public class ActorSystem {

    private final String name;

    private final int workerPoolSize = 4;

    private final BlockingQueue<Runnable>[] workerJobs = new BlockingQueue[this.workerPoolSize];
    private final ExecutorService workers = Executors.newFixedThreadPool(this.workerPoolSize);
    private final ApplicationSupervisor[] applicationSupervisors = new ApplicationSupervisor[this.workerPoolSize];
    private final TransactionSupervisor[] transactionSupervisors = new TransactionSupervisor[this.workerPoolSize];
    private final TransportSupervisor[] transpotSupervisors = new TransportSupervisor[this.workerPoolSize];
    private final PipeLineFactory[] inboundPipefactory = new PipeLineFactory[this.workerPoolSize];

    /**
     * 
     */
    public ActorSystem(final String name) {
        this.name = name;

        for (int i = 0; i < this.workerJobs.length; ++i) {
            final BlockingQueue<Runnable> jobQueue = new LinkedBlockingQueue<Runnable>(100);
            this.workerJobs[i] = jobQueue;

            final TransportSupervisor transportSupervisor = new TransportSupervisor();
            this.transpotSupervisors[i] = transportSupervisor;

            final TransactionSupervisor transactionSupervisor = new TransactionSupervisor();
            this.transactionSupervisors[i] = transactionSupervisor;

            // TODO: insert DialogSupervisor if configured to do so.

            final ApplicationSupervisor applicationSupervisor = new ApplicationSupervisor();
            this.applicationSupervisors[i] = applicationSupervisor;

            this.inboundPipefactory[i] =
                    PipeLineFactory.withDefaultChain(transportSupervisor, transactionSupervisor, applicationSupervisor);

            final Worker worker = new Worker(i, jobQueue);
            this.workers.execute(worker);
        }
    }

    /**
     * Dispatch an inbound event using the pre-configured inbound pipe factory.
     * 
     * @param event
     */
    public void dispatchInboundEvent(final Event event) {
        final Key key = event.key();
        final int worker = Math.abs(key.hashCode() % this.workerPoolSize);
        final PipeLineFactory factory = this.inboundPipefactory[worker];
        final PipeLine pipeLine = factory.newPipeLine();
        final DispatchJob job = new DispatchJob(this, worker, pipeLine, event);
        if (!this.workerJobs[worker].offer(job)) {
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
        final DispatchJob job = new DispatchJob(this, worker, pipeLine, event);
        if (!this.workerJobs[worker].offer(job)) {
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
