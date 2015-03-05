/**
 * 
 */
package io.sipstack.actor;

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

    Optional<PipeLine> replace(Actor toBeReplaced, Actor replacement);

    Optional<PipeLine> remove(Actor toRemove);

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


    static PipeLine withChain(final List<Actor> chain) {
        return new DefaultPipeLine(chain);
    }

    static PipeLine withChain(final Actor... actors) {
        return new DefaultPipeLine(Arrays.asList(actors));
    }

    static class DefaultPipeLine implements PipeLine {

        private final List<Actor> chain;
        private final int next;

        private DefaultPipeLine(final List<Actor> chain) {
            this(0, chain);
        }

        private DefaultPipeLine(final int next, final List<Actor> chain) {
            this.next = next;
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
        public Optional<PipeLine> replace(final Actor toBeReplaced, final Actor replacement) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Optional<PipeLine> remove(final Actor toRemove) {
            // TODO Auto-generated method stub
            return null;
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

    }
}
