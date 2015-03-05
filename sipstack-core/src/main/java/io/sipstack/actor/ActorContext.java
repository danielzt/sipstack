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
        private PipeLine getInboundPipe() {
            return isInbound ? this.pipeLine : this.pipeLine.reverse();
        }

        /**
         * Same as {@link #getInboundPipe(PipeLine)} but different :-)
         * 
         * @param pipeLine
         * @return
         */
        private PipeLine getOutboundPipe() {
            return isInbound ? this.pipeLine.reverse() : this.pipeLine;
        }

        private void dispatchEvent(final boolean inbound, final Event event) {
            final PipeLine pipeLine = inbound ? getInboundPipe() : getOutboundPipe();
            final Optional<Actor> optional = pipeLine.next();

            if (!optional.isPresent()) {
                return;
            }

            final Actor next = optional.get();
            final BufferedActorContext bufferedCtx = invokeActor(inbound, next, pipeLine, event);

            final List<Event> downstreamEvents = bufferedCtx.getDownstreamEvents();
            if (!downstreamEvents.isEmpty()) {
                // if this is an inbound event then we have to get the outbound
                // pipe first and progress on it. If this already was an outbound
                // event than the 'pipeLine' is our outbound pipe so no reason
                // to call getOutboundPipe() again. Waste of time and resources...
                final PipeLine nextOutboundPipe = inbound ? getOutboundPipe().progress() : pipeLine.progress();
                for (final Event downstream : downstreamEvents) {
                    final ActorContext nextCtx = ActorContext.withOutboundPipeLine(nextOutboundPipe);
                    nextCtx.fireDownstreamEvent(downstream);
                }
            }

            final List<Event> upstreamEvents = bufferedCtx.getUpstreamEvents();
            if (!upstreamEvents.isEmpty()) {
                final PipeLine nextInboundPipe = inbound ? pipeLine.progress() : getInboundPipe().progress();
                for (final Event upstream : bufferedCtx.getUpstreamEvents()) {
                    final ActorContext nextCtx = ActorContext.withInboundPipeLine(nextInboundPipe);
                    nextCtx.fireUpstreamEvent(upstream);
                }
            }
        }

        /**
         * Whenever we invoke an {@link Actor} that actor can of course blow up but we should under
         * no circumstances allow that exception to escape but rather catch it and push that as an
         * event through the {@link PipeLine}. If it did escape, it could eventually kill out
         * thread, which wouldn't be all that fun...
         * 
         * @param next
         * @param pipeLine
         * @return
         */
        private BufferedActorContext invokeActor(final boolean inbound, final Actor next, final PipeLine pipeLine,
                final Event event) {
            final BufferedActorContext bufferedCtx = new BufferedActorContext(pipeLine);
            try {
                if (inbound) {
                    next.onUpstreamEvent(bufferedCtx, event);
                } else {
                    next.onDownstreamEvent(bufferedCtx, event);
                }
            } catch (final Throwable t) {
                System.err.println("Do something about it...");
                t.printStackTrace();
            }
            return bufferedCtx;
        }

    }

    /**
     * Acts as a place holder when we invoke the next {@link Actor}. The reason is that whenever an
     * {@link Actor} is invoked it can produce new events but our contract guarantees that no event
     * is actually never ever processed until the {@link Actor#onUpstreamEvent(ActorContext, Event)} has
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
