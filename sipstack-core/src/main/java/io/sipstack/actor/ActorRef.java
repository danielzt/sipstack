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

    /**
     * Check which thread a particular Key would run on if dispatched.
     *
     * @param key
     * @return
     */
    int willRunOnThread(Key key);

    /**
     * Check whether a task with the given key will be executed on the same worker thread as "me".
     *
     * @param key
     * @return
     */
    boolean willExecuteOnSameThread(Key key);

    /**
     * @param workerPool
     * @return
     */
    static Builder withWorkerPool(final int workerPool) {
        PreConditions.assertArgument(workerPool >= 0 || workerPool == -1, "Worker Pool has to be greater or equal to zero");
        return new Builder(workerPool);
    }

    static class Builder {
        private final int workerPool;
        private String name;
        private int noOfWorkers;
        private ActorRef parent;

        private Builder(final int workerPool) {
            this.workerPool = workerPool;
        }

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withParent(final ActorRef parent) {
            this.parent = parent;
            return this;
        }

        public Builder withNoOfWorkers(final int noOfWorkers) {
            this.noOfWorkers = noOfWorkers;
            return this;
        }

        public ActorRef build() {
            PreConditions.ensureNotEmpty(this.name, "You must sepcify a name for the actor");
            PreConditions.assertArgument(noOfWorkers > 0, "The number of workers must be greater than zero");
            return new DefaultActorRef(parent, noOfWorkers, workerPool, name);
        }
    }

    static class DefaultActorRef implements ActorRef {
        private final int noOfWorkers;
        private final int workerPool;
        private final String name;
        private final ActorRef parent;

        private DefaultActorRef(ActorRef parent, final int noOfWorkers, final int workerPool, final String name) {
            this.parent = parent;
            this.noOfWorkers = noOfWorkers;
            this.workerPool = workerPool;
            this.name = name;
        }

        @Override
        public int workerPool() {
            return workerPool;
        }

        @Override
        public int willRunOnThread(Key key) {
            return Math.abs(key.hashCode() % noOfWorkers);
        }

        @Override
        public boolean willExecuteOnSameThread(Key key) {
            return workerPool == willRunOnThread(key);
        }


    }

}
