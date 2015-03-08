/**
 * 
 */
package io.sipstack.actor;

import io.sipstack.netty.codec.sip.SipMessageEvent;
import io.sipstack.transaction.impl.TransactionSupervisor;

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
    private final TransactionSupervisor[] transactionSupervisors = new TransactionSupervisor[this.workerPoolSize];
    private final PipeLineFactory[] inboundPipefactory = new PipeLineFactory[this.workerPoolSize];

    /**
     * 
     */
    public ActorSystem(final String name) {
        this.name = name;

        for (int i = 0; i < this.workerJobs.length; ++i) {
            final BlockingQueue<Runnable> jobQueue = new LinkedBlockingQueue<Runnable>(100);
            this.workerJobs[i] = jobQueue;

            final TransactionSupervisor supervisor = new TransactionSupervisor();
            this.transactionSupervisors[i] = supervisor;
            this.inboundPipefactory[i] = PipeLineFactory.withDefaultChain(supervisor);

            final Worker worker = new Worker(i, jobQueue);
            this.workers.execute(worker);
        }
    }

    public void receive(final SipMessageEvent event) {

        final SipEvent sipEvent = SipEvent.create(event);
        final Key key = sipEvent.key();
        final int worker = Math.abs(key.hashCode() % this.workerPoolSize);
        final PipeLineFactory factory = this.inboundPipefactory[worker];
        final DispatchJob job = new DispatchJob(factory, sipEvent);

        if (!this.workerJobs[worker].offer(job)) {
            // TODO: handle non accepted job
        }
    }

    private static final class DispatchJob implements Runnable {
        private final PipeLineFactory factory;
        private final Event event;

        public DispatchJob(final PipeLineFactory factory, final Event event) {
            this.factory = factory;
            this.event = event;
        }

        @Override
        public void run() {
            final PipeLine pipe = this.factory.newPipeLine();
            final ActorContext ctx = ActorContext.withPipeLine(pipe);
            ctx.fireUpstreamEvent(this.event);
        }
    }

}
