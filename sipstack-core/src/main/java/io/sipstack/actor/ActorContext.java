/**
 * 
 */
package io.sipstack.actor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ActorContext {

    ActorContext replace(Actor toBeReplaced, Actor replaceWith);

    void fireUpstreamEvent(Event event);

    void fireDownstreamEvent(Event event);

    static ActorContext withPipeLine(final PipeLine pipeLine) {
        return new DefaultActorContext(true, pipeLine);
    }

    static ActorContext withInboundPipeLine(final PipeLine pipeLine) {
        return new DefaultActorContext(true, pipeLine);
    }

    static ActorContext withOutboundPipeLine(final PipeLine pipeLine) {
        return new DefaultActorContext(false, pipeLine);
    }


    static class DefaultActorContext implements ActorContext {

        private final PipeLine pipeLine;

        private final boolean isInbound;

        private DefaultActorContext(final boolean isInbound, final PipeLine pipeLine) {
            this.isInbound = isInbound;
            this.pipeLine = pipeLine;
        }

        @Override
        public ActorContext replace(final Actor toBeReplaced, final Actor replaceWith) {
            // final PipeLine pipe = this.pipeLine.replace(toBeReplaced, replaceWith);
            // final ActorContext ctx = ActorContext.withPipeLine(pipe);
            // return ctx;
            throw new RuntimeException("Not implemented yet");
        }


        @Override
        public void fireUpstreamEvent(final Event event) {
            dispatchEvent(true, event);
        }

        @Override
        public void fireDownstreamEvent(final Event event) {
            dispatchEvent(false, event);
        }

        /**
         * If the current "direction" of this {@link ActorContext} is inbound, then this method will
         * return the current configured {@link PipeLine}. However, if the direction is outbound
         * then we have to reverse the pipe so that it becomes an "inbound" pipe.
         * 
         * @param pipeLine
         * @return
         */
        private PipeLine getInboundPipe(final PipeLine pipeLine) {
            return isInbound ? this.pipeLine : this.pipeLine.reverse();
        }

        /**
         * Same as {@link #getInboundPipe(PipeLine)} but different :-)
         * 
         * @param pipeLine
         * @return
         */
        private PipeLine getOutboundPipe(final PipeLine pipeLine) {
            return isInbound ? this.pipeLine.reverse() : this.pipeLine;
        }

        private void dispatchEvent(final boolean inbound, final Event event) {
            final PipeLine inboundPipe = getInboundPipe(this.pipeLine);
            final PipeLine outboundPipe = getOutboundPipe(this.pipeLine);

            final Optional<Actor> next = inbound ? inboundPipe.next() : outboundPipe.next();

            if (!next.isPresent()) {
                return;
            }

            final BufferedActorContext bufferedCtx = new BufferedActorContext(pipeLine);
            next.get().onEvent(bufferedCtx, event);

            final PipeLine nextOutboundPipe = outboundPipe.progress();
            for (final Event downstream : bufferedCtx.getDownstreamEvents()) {
                final ActorContext nextCtx = ActorContext.withPipeLine(nextOutboundPipe);
                nextCtx.fireDownstreamEvent(downstream);
            }

            final PipeLine nextInboundPipe = inboundPipe.progress();
            for (final Event upstream : bufferedCtx.getUpstreamEvents()) {
                final ActorContext nextCtx = ActorContext.withPipeLine(nextInboundPipe);
                nextCtx.fireUpstreamEvent(upstream);
            }
        }
    }

    /**
     * Acts as a place holder when we invoke the next {@link Actor}. The reason is that whenever an
     * {@link Actor} is invoked it can produce new events but our contract guarantees that no event
     * is actually never ever processed until the {@link Actor#onEvent(ActorContext, Event)} has
     * returned. This makes it much easier to reason about what happens in the system and also makes
     * it way easier to test!
     * 
     */
    static class BufferedActorContext implements ActorContext {

        private List<Event> upstreamEvents;
        private List<Event> downstreamEvents;

        private final PipeLine pipeLine;

        private BufferedActorContext(final PipeLine pipeLine) {
            this.pipeLine = pipeLine;
        }

        protected List<Event> getUpstreamEvents() {
            if( upstreamEvents != null) {
                return upstreamEvents;
            }
            return Collections.emptyList();
        }

        protected List<Event> getDownstreamEvents() {
            if (downstreamEvents != null) {
                return downstreamEvents;
            }
            return Collections.emptyList();
        }

        @Override
        public ActorContext replace(final Actor toBeReplaced, final Actor replaceWith) {
            return this;
        }

        @Override
        public void fireUpstreamEvent(final Event event) {
            if (upstreamEvents == null) {
                this.upstreamEvents = new ArrayList<Event>();
            }
            this.upstreamEvents.add(event);
        }


        @Override
        public void fireDownstreamEvent(final Event event) {
            if (downstreamEvents == null) {
                this.downstreamEvents = new ArrayList<Event>();
            }
            this.downstreamEvents.add(event);
        }
    }


}
