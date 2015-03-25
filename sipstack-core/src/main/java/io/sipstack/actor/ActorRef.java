/**
 * 
 */
package io.sipstack.actor;

import io.pkts.packet.sip.impl.PreConditions;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ActorRef {

    /**
     * The worker pool this actor belongs to.
     * 
     * @return
     */
    int workerPool();

    static Builder withWorkerPool(final int workerPool) {
        PreConditions.assertArgument(workerPool >= 0, "Worker Pool has to be greater or equal to zero");
        return new Builder(workerPool);
    }

    static class Builder {
        private final int workerPool;
        private String name;

        private Builder(final int workerPool) {
            this.workerPool = workerPool;
        }

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public ActorRef build() {
            PreConditions.ensureNotEmpty(this.name, "You must sepcify a name for the actor");
            return new DefaultActorRef(workerPool, name);
        }
    }

    static class DefaultActorRef implements ActorRef {
        private final int workerPool;
        private final String name;

        private DefaultActorRef(final int workerPool, final String name) {
            this.workerPool = workerPool;
            this.name = name;
        }

        @Override
        public int workerPool() {
            return this.workerPool;
        }

    }

}
