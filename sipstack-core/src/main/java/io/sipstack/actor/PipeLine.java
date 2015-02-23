/**
 * 
 */
package io.sipstack.actor;

import java.util.ArrayList;
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
     * @return a new {@link PipeLine} that is one step ahead of this one.
     */
    PipeLine progress();

    Optional<Actor> next();

    /**
     * Turn the pipe around based on the current location. The current location will be preserved
     * but is acting like it is going in the reverse direction compared to the original one.
     * 
     * @return a new {@link PipeLine} that is in the reverse order compared to this one.
     */
    PipeLine reverse();


    static PipeLine withChain(final List<Actor> chain) {
        return new DefaultPipeLine(chain);
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
            if (next < chain.size()) {
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
            return new DefaultPipeLine(next + 1, this.chain);
        }

        @Override
        public PipeLine reverse() {
            try {
                final List<Actor> reverse = new ArrayList<>(next + 1);
                for (int i = Math.min(next, chain.size() - 1); i >= 0; --i) {
                    reverse.add(chain.get(i));
                }
                return new DefaultPipeLine(0, reverse);
            } catch (final IndexOutOfBoundsException e) {
                throw e;
            }
        }

    }
}
