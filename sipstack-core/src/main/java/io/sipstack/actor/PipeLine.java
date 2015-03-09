/**
 * 
 */
package io.sipstack.actor;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A {@link PipeLine} is a simple chain of {@link Actor}s. A {@link PipeLine} only goes in one
 * direction, forward but allows to reverse the order of the pipe but once again, to that new
 * reversed {@link PipeLine}, the direction is still forward.
 * 
 * @author jonas@jonasborjesson.com
 *
 */
public interface PipeLine {

    /**
     * Replace the {@link Actor} at the current position. The new {@link PipeLine} that is returned
     * will have the exact same chain as this {@link PipeLine} with the difference of the actor at
     * the current index, hence, once the current is replaced, a call to {@link #next()} will
     * naturally return the actor we just passed in.
     * 
     * Of course, since {@link PipeLine}s are immutable, the current pipe line will still containt
     * the original chain.
     * 
     * @param actor
     * @return
     * @throws IllegalArgumentException in case the actor is null.
     */
    PipeLine replaceCurrent(Actor actor) throws IllegalArgumentException;

    /**
     * Remove the actor at the current position and return a new {@link PipeLine} reflecting that
     * change.
     * 
     * If the {@link PipeLine} only had a single element, an empty PipeLine will be returned. throws
     * IllegalArgumentException; If the current position is the last position in the pipeline, then
     * new pipeline will be at the end since we just removed the last one.
     * 
     * @return
     */
    PipeLine removeCurrent();

    /**
     * Insert a new Actor into the pipe. The new actor will be inserted after the current location,
     * which means that calling {@link #next()} on the newly returned {@link PipeLine} will still
     * yield the same as this one. However, calling {@link #progress()} and then {@link #next()}
     * will return the newly inserted actor.
     * 
     * @param actor
     * @return
     * @throws IllegalArgumentException in case the actor is null.
     */
    PipeLine insert(Actor actor) throws IllegalArgumentException;

    /**
     * Calling {@link #progress()} will force this {@link PipeLine} to progress forward. However,
     * since a {@link PipeLine} is immutable, a new {@link PipeLine} is returned, which is one step
     * ahead of this one.
     * 
     * Note that is is possible to walk off of the pipe, i.e., if you are at the last element in the
     * pipe and call {@link #progress()} you will "walk off" and therefore {@link #next()} will
     * return nothing. This is by design and allows you to detect when there are no more elements in
     * the pipe and you can still at this point {@link #reverse()} the pipe and walk back the same
     * way you came.
     * 
     * @return a new {@link PipeLine} that is one step ahead of this one.
     */
    PipeLine progress();

    /**
     * Calling {@link #regress()} will force this {@link PipeLine} to move backwards. However, since
     * a {@link PipeLine} is immutable, a new {@link PipeLine} is returned, which is one step behind
     * this one.
     * 
     * @return
     */
    PipeLine regress();

    Optional<Actor> next();

    /**
     * Turn the pipe around based on the current location. The current location will be preserved
     * but is acting like it is going in the reverse direction compared to the original one.
     * 
     * Example, if you have the following chain:
     * 
     * <pre>
     *   A -> B -> C -> D
     *   ^
     *   |
     * next
     * </pre>
     * 
     * So you are currently pointing to the first element, which is your next, and if you reverse
     * this around you will get:
     * 
     * <pre>
     *   D -> C -> B -> A
     *                  ^
     *                  |
     *                 next
     * </pre>
     * 
     * Which would mean that next is still A but if you do {@link #progress()} you will in fact walk
     * off the queue.
     * 
     * More commonly, you would have a situation where you have walked a portion, or all, of the
     * chain and at that point you reverse like so:
     * 
     * <pre>
     *   A -> B -> C -> D
     *             ^
     *             |
     *            next
     * </pre>
     * 
     * after {@link #reverse()}
     * 
     * <pre>
     *   0    1    2    3
     *   D -> C -> B -> A
     *        ^
     *        |
     *       next
     * </pre>
     * 
     * {@link #next()} will once again return C but now when you {@link #progress()} you will walk
     * back the pipe compared to the previous direction.
     * 
     * @return a new {@link PipeLine} that is in the reverse order compared to this one.
     */
    PipeLine reverse();


    static PipeLine empty() {
        return DefaultPipeLine.EMPTY;
    }

    static PipeLine withChain(final List<Actor> chain) {
        return new DefaultPipeLine(chain);
    }

    static PipeLine withChain(final Actor... actors) {
        return new DefaultPipeLine(Arrays.asList(actors));
    }

    static class DefaultPipeLine implements PipeLine {

        private static final PipeLine EMPTY = new EmptyPipeLine();

        private final List<Actor> chain;
        private final int next;

        private DefaultPipeLine(final List<Actor> chain) {
            this(0, chain);
        }

        private DefaultPipeLine(final int next, final List<Actor> chain) {
            this.next = next < -1 ? -1 : next;
            this.chain = chain;
        }


        @Override
        public Optional<Actor> next() {
            if (next >= 0 && next < chain.size()) {
                return Optional.of(chain.get(next));
            }
            return Optional.empty();
        }

        @Override
        public PipeLine replaceCurrent(final Actor actor) throws IllegalArgumentException {
            ensureNotNull(actor);
            final List<Actor> newChain = new ArrayList<>(chain.size());
            for (int i = 0; i < chain.size(); ++i) {
                if (i == next) {
                    newChain.add(actor);
                } else {
                    newChain.add(chain.get(i));
                }
            }
            return new DefaultPipeLine(next, newChain);
        }

        @Override
        public PipeLine removeCurrent() {
            final int size = chain.size() - 1;
            if (size <= 0) {
                return EMPTY;
            }

            final List<Actor> newChain = new ArrayList<>(size);
            for (int i = 0; i < chain.size(); ++i) {
                if (i != next) {
                    newChain.add(chain.get(i));
                }
            }
            return new DefaultPipeLine(next, newChain);
        }

        @Override
        public PipeLine progress() {
            return new DefaultPipeLine(Math.min(next + 1, chain.size()), this.chain);
        }

        @Override
        public PipeLine reverse() {
            try {
                final List<Actor> reverse = new ArrayList<>(chain.size());
                for (int i = chain.size() - 1; i >= 0; --i) {
                    reverse.add(chain.get(i));
                }

                return new DefaultPipeLine(chain.size() - next - 1, reverse);
            } catch (final IndexOutOfBoundsException e) {
                throw e;
            }
        }

        @Override
        public PipeLine insert(final Actor actor) throws IllegalArgumentException {
            ensureNotNull(actor);
            final List<Actor> newChain = new ArrayList<>(chain.size() + 1);
            for (int i = 0; i < chain.size(); ++i) {
                newChain.add(chain.get(i));
                if (i == next) {
                    newChain.add(actor);
                }
            }
            return new DefaultPipeLine(next, newChain);
        }

        @Override
        public PipeLine regress() {
            return new DefaultPipeLine(next - 1, this.chain);
        }
    }

    static class EmptyPipeLine implements PipeLine {

        @Override
        public PipeLine replaceCurrent(final Actor actor) throws IllegalArgumentException {
            ensureNotNull(actor);
            return PipeLine.withChain(actor);
        }

        @Override
        public PipeLine removeCurrent() {
            return this;
        }

        /**
         * Remember that the javadoc says that insert will insert one ahead of the current location
         * so that means that {@link #next()} should still return empty.
         * 
         * {@inheritDoc}
         */
        @Override
        public PipeLine insert(final Actor actor) throws IllegalArgumentException {
            ensureNotNull(actor);
            return new DefaultPipeLine(-1, Arrays.asList(actor));
        }

        @Override
        public PipeLine progress() {
            return this;
        }

        @Override
        public Optional<Actor> next() {
            return Optional.empty();
        }

        @Override
        public PipeLine reverse() {
            return this;
        }

        @Override
        public PipeLine regress() {
            return this;
        }

    }
}
