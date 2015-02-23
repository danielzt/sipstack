/**
 * 
 */
package io.sipstack.actor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author jonas@jonasborjesson.com
 * 
 */
public interface PipeLineFactory {


    PipeLine newPipeLine();

    static PipeLineFactory withDefaultChain(final List<Actor> actors) {
        return new DefaultPipeLineFactory(Collections.unmodifiableList(actors));
    }

    static PipeLineFactory withDefaultChain(final Actor... actors) {
        final List<Actor> chain = Arrays.asList(actors);
        return new DefaultPipeLineFactory(chain);
    }

    static class DefaultPipeLineFactory implements PipeLineFactory {
        private final List<Actor> chain;

        private DefaultPipeLineFactory(final List<Actor> chain) {
            this.chain = chain;
        }

        @Override
        public PipeLine newPipeLine() {
            return PipeLine.withChain(chain);
        }
    }


}
