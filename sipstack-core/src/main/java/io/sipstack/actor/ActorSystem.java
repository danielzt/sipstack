/**
 * 
 */
package io.sipstack.actor;

import io.sipstack.netty.codec.sip.SipMessageEvent;
import io.sipstack.transaction.TransactionSupervisor;

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

    private final BlockingQueue<Event>[] workerJobs = new BlockingQueue[this.workerPoolSize];
    private final ExecutorService workers = Executors.newFixedThreadPool(this.workerPoolSize);
    private final TransactionSupervisor[] transactionSupervisors = new TransactionSupervisor[this.workerPoolSize];

    /**
     * 
     */
    public ActorSystem(final String name) {
        this.name = name;

        for (int i = 0; i < this.workerJobs.length; ++i) {
            final BlockingQueue<Event> jobQueue = new LinkedBlockingQueue<Event>(100);
            this.workerJobs[i] = jobQueue;
            final TransactionSupervisor supervisor = new TransactionSupervisor();

            final PipeLineFactory inboundPipe = PipeLineFactory.withDefaultChain(supervisor);
            this.transactionSupervisors[i] = supervisor;

            final Worker worker = new Worker(i, inboundPipe, jobQueue);
            this.workers.execute(worker);
        }
    }

    public void receive(final SipMessageEvent event) {
        final Event newEvent = new SipEvent(event);
        // final Buffer callId = event.getMessage().getCallIDHeader().getValue();
        final Key key = newEvent.key();
        final int worker = Math.abs(key.hashCode() % this.workerPoolSize);
        if (!this.workerJobs[worker].offer(newEvent)) {
            // TODO: handle non accepted job
        }
    }

}
