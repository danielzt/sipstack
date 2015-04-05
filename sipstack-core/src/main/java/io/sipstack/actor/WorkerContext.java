/**
 * 
 */
package io.sipstack.actor;

import io.pkts.packet.sip.impl.PreConditions;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author jonas@jonasborjesson.com
 */
public interface WorkerContext {

    BlockingQueue<Runnable> workerQueue();

    PipeLineFactory defaultPipeLineFactory();

    static Builder withQueue(final BlockingQueue<Runnable> queue) {
        final Builder builder = new Builder();
        builder.withQueue(queue);
        return builder;
    }

    static Builder withDefaultChain(final Actor... actors) {
        final Builder builder = new Builder();
        builder.withDefaultChain(actors);
        return builder;
    }

    static class Builder {

        private BlockingQueue<Runnable> queue;
        private PipeLineFactory pipeLineFactory;

        private Builder() {
            // left empty intentionally
        }

        public Builder withQueue(final BlockingQueue<Runnable> queue) {
            this.queue = queue;
            return this;
        }

        public Builder withDefaultChain(final Actor... actors) {
            if (actors == null || actors.length == 0) {
                throw new IllegalArgumentException("You must specify at least one actor in the chain");
            }

            this.pipeLineFactory = PipeLineFactory.withDefaultChain(actors);
            return this;
        }

        public WorkerContext build() {
            PreConditions.ensureNotNull(pipeLineFactory, "You must specity a default actor chain");
            // final BlockingQueue<Runnable> jobQueue = queue != null ? queue : new LinkedBlockingQueue<Runnable>(100);
            final BlockingQueue<Runnable> jobQueue = queue != null ? queue : new ConcurrentLinkedDeque<>()
            return new DefaultWorkerContext(jobQueue, pipeLineFactory);
        }
    }

    static class DefaultWorkerContext implements WorkerContext {
        private final BlockingQueue<Runnable> queue;
        private final PipeLineFactory pipeLineFactory;

        private DefaultWorkerContext(final BlockingQueue<Runnable> queue, final PipeLineFactory pipeLineFactory) {
            this.queue = queue;
            this.pipeLineFactory = pipeLineFactory;
        }

        @Override
        public BlockingQueue<Runnable> workerQueue() {
            return queue;
        }

        @Override
        public PipeLineFactory defaultPipeLineFactory() {
            return pipeLineFactory;
        }

    }

}
