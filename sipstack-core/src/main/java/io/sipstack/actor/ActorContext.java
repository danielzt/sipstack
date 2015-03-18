/**
 * 
 */
package io.sipstack.actor;

import io.sipstack.event.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ActorContext {

    /**
     * Replace the current {@link Actor} with this one. The next {@link #forwardUpstreamEvent(Event)}
     * will go through the newly inserted actor.
     * 
     * @param actor
     * @return
     */
    ActorContext replace(Actor actor);

    /**
     * Whenever an {@link Actor} wish to be disposed of, just call this method and the system will
     * ensure to kill off the {@link Actor}.
     * 
     * It is guaranteed that any upstream and/or downstream events that the {@link Actor} generated
     * during its invocation will first be dispatched before the Actor is killed off.
     */
    void killMe();

    /**
     * When your {@link Actor} is done processing its {@link Event} it can decide to forward the
     * same (or a new) event upstream. There are two methods for forwarding an event upstream, the
     * {@link #forwardUpstreamEvent(Event)} and the {@link #fireUpstreamEvent(Event)} and the
     * difference is that the former will use the same thread as this {@link Actor} is currently
     * using while the latter may select another thread to run on as decided by the
     * {@link Event#key()}
     * 
     * Usually, if you are sure that the same thread should handle the next execution in the
     * pipeline (because e.g. the very same key is used, which is true for all the actors handling a
     * sip message) then use {@link #forwardUpstreamEvent(Event)} but if you are not sure, or if a
     * different key indeed is used, then use the {@link #fireUpstreamEvent(Event)} instead.
     * 
     * @param event
     */
    void forwardUpstreamEvent(Event event);

    void forwardDownstreamEvent(Event event);

    void fireUpstreamEvent(Event event);

    void fireDownstreamEvent(Event event);

    static ActorContext withInboundPipeLine(final ActorSystem system, final PipeLine pipeLine) {
        return new DefaultActorContext(system, true, pipeLine);
    }

    static ActorContext withOutboundPipeLine(final ActorSystem system, final PipeLine pipeLine) {
        return new DefaultActorContext(system, false, pipeLine);
    }


    /**
     * The {@link DefaultActorContext} is what we work with internally and is really only created
     * and used by the {@link ActorSystem} itself.
     * 
     * @author jonas@jonasborjesson.com
     *
     */
    static class DefaultActorContext implements ActorContext {

        private final ActorSystem system;

        private final PipeLine pipeLine;

        private final boolean isInbound;

        private DefaultActorContext(final ActorSystem actorSystem, final boolean isInbound, final PipeLine pipeLine) {
            this.system = actorSystem;
            this.isInbound = isInbound;
            this.pipeLine = pipeLine;
        }

        @Override
        public ActorContext replace(final Actor actor) {
            // final PipeLine pipe = this.pipeLine.replace(toBeReplaced, replaceWith);
            // final ActorContext ctx = ActorContext.withPipeLine(pipe);
            // return ctx;
            throw new RuntimeException("Not implemented yet");
        }


        @Override
        public void forwardUpstreamEvent(final Event event) {
            dispatchEvent(true, event);
        }

        @Override
        public void forwardDownstreamEvent(final Event event) {
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
                    final ActorContext nextCtx = ActorContext.withOutboundPipeLine(this.system, nextOutboundPipe);
                    nextCtx.forwardDownstreamEvent(downstream);
                }
            }

            final List<Event> upstreamEvents = bufferedCtx.getUpstreamEvents();
            final List<Event> continueUpstreamEvents = bufferedCtx.getContinueUpstreamEvents();

            if (!upstreamEvents.isEmpty() || !continueUpstreamEvents.isEmpty()) {
                PipeLine nextInboundPipe = inbound ? pipeLine : getInboundPipe();
                if (bufferedCtx.getNewActor() != null) {
                    nextInboundPipe = nextInboundPipe.insert(bufferedCtx.getNewActor()).removeCurrent();
                } else {
                    nextInboundPipe = nextInboundPipe.progress();
                }

                for (final Event upstream : upstreamEvents) {
                    final ActorContext nextCtx = ActorContext.withInboundPipeLine(this.system, nextInboundPipe);
                    nextCtx.forwardUpstreamEvent(upstream);
                }

                for (final Event upstream : continueUpstreamEvents) {
                    this.system.dispatchEvent(upstream, nextInboundPipe);
                }
            }

            // if the actor have asked to die, then kill it off by telling its parent.
            // Note, because of how things are setup, an actor that asked to be disposed of will
            // ALWAYS already be executing on the correct thread pool which means that we can take
            // advantage of that here.
            if (bufferedCtx.disposeOfActor()) {
                final Supervisor supervisor = next.getSupervisor();
                supervisor.killChild(next);
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
                t.printStackTrace();
            }
            return bufferedCtx;
        }

        @Override
        public void killMe() {
            throw new RuntimeException("You cannot call killMe outside of an Actor invocation");
        }

        @Override
        public void fireUpstreamEvent(final Event event) {
            throw new RuntimeException("You shouldn't call this method outside of an Actor invocation");
        }

        @Override
        public void fireDownstreamEvent(final Event event) {
            throw new RuntimeException("You shouldn't call this method outside of an Actor invocation");
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

        private List<Event> continueUpstreamEvents;
        private List<Event> continueDownstreamEvents;

        /**
         * If the user calls {@link #replace(Actor)} then this is that actor that the user wished to
         * replace the current one with.
         */
        private Actor newActor;

        private final PipeLine pipeLine;

        private boolean killMe = false;

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

        protected List<Event> getContinueDownstreamEvents() {
            if (continueDownstreamEvents != null) {
                return continueDownstreamEvents;
            }
            return Collections.emptyList();
        }

        protected List<Event> getContinueUpstreamEvents() {
            if (continueUpstreamEvents != null) {
                return continueUpstreamEvents;
            }
            return Collections.emptyList();
        }

        protected Actor getNewActor() {
            return this.newActor;
        }

        protected boolean disposeOfActor() {
            return this.killMe;
        }

        @Override
        public ActorContext replace(final Actor actor) {
            this.newActor = actor;
            return this;
        }

        @Override
        public void forwardUpstreamEvent(final Event event) {
            if (upstreamEvents == null) {
                this.upstreamEvents = new ArrayList<>();
            }
            this.upstreamEvents.add(event);
        }


        @Override
        public void forwardDownstreamEvent(final Event event) {
            if (downstreamEvents == null) {
                this.downstreamEvents = new ArrayList<>();
            }
            this.downstreamEvents.add(event);
        }

        @Override
        public void killMe() {
            this.killMe = true;
        }

        @Override
        public void fireUpstreamEvent(final Event event) {
            if (continueUpstreamEvents == null) {
                this.continueUpstreamEvents = new ArrayList<>();
            }
            this.continueUpstreamEvents.add(event);
        }

        @Override
        public void fireDownstreamEvent(final Event event) {
            if (continueDownstreamEvents == null) {
                this.continueDownstreamEvents = new ArrayList<>();
            }
            this.continueDownstreamEvents.add(event);
        }
    }


}
