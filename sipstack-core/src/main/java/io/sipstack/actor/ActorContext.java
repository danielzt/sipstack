/**
 * 
 */
package io.sipstack.actor;

import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.actor.ActorSystem.DefaultActorSystem.DispatchJob;
import io.sipstack.config.Configuration;
import io.sipstack.event.Event;
import io.sipstack.event.SipTimerEvent;
import io.sipstack.timers.SipTimer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ActorContext {

    /**
     * Replace the current {@link Actor} with this one. The next {@link #forward(Event)}
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
     * {@link #forward(Event)} and the {@link #forward(Event)} and the
     * difference is that the former will use the same thread as this {@link Actor} is currently
     * using while the latter may select another thread to run on as decided by the
     * {@link Event#key()}
     * 
     * Usually, if you are sure that the same thread should handle the next execution in the
     * pipeline (because e.g. the very same key is used, which is true for all the actors handling a
     * sip message) then use {@link #forward(Event)} but if you are not sure, or if a
     * different key indeed is used, then use the {@link #fire(Event)} instead.
     * 
     * @param event
     */
    // void forwardUpstreamEvent(Event event);

    // void forwardDownstreamEvent(Event event);

    // void fireUpstreamEvent(Event event);

    // void fireDownstreamEvent(Event event);

    void forward(Event event);

    void fire(Event event);

    ActorContext reverse();

    Scheduler scheduler();

    Configuration getConfig();

    static ActorContext withPipeLine(final int workerThread, final ActorSystem system, final PipeLine pipeLine) {
        PreConditions.ensureArgument(workerThread >= 0, "The worker thread must >= 0");
        return new DefaultActorContext(workerThread, system, pipeLine);
    }

    // static ActorContext withInboundPipeLine(final ActorSystem system, final PipeLine pipeLine) {
    // return new DefaultActorContext(system, true, pipeLine);
    // }

    // static ActorContext withOutboundPipeLine(final ActorSystem system, final PipeLine pipeLine) {
    // return new DefaultActorContext(system, false, pipeLine);
    // }


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

        /**
         * The worker thread we are currently executing on as in, this is the "index" of the worker
         * thread that we are on and that is managed by the {@link ActorSystem}. Note, this is
         * internal implementation detail only and is never ever exposed to the applications.
         */
        private final int workerThread;

        private DefaultActorContext(final int workerThread, final ActorSystem actorSystem, final PipeLine pipeLine) {
            this.workerThread = workerThread;
            this.system = actorSystem;
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
        public void forward(final Event event) {
            final Optional<Actor> optional = pipeLine.next();

            if (!optional.isPresent()) {
                return;
            }

            // process the reversed context first because we want to
            // prioritize events coming back as a response to another event.
            // E.g., if an actor gets an INVITE and generates a 100 Trying we want
            // to get that Trying out before the INVITE propagates up to the next
            // actor.
            final Actor next = optional.get();
            final BufferedActorContext bufferedCtx = invokeActor(next, pipeLine, event);
            processCtx(bufferedCtx.reversedCtx);
            processCtx(bufferedCtx);

            // if the actor have asked to die, then kill it off by telling its parent.
            // Note, because of how things are setup, an actor that asked to be disposed of will
            // ALWAYS already be executing on the correct thread pool which means that we can take
            // advantage of that here.
            if (bufferedCtx.disposeOfActor()) {
                final Supervisor supervisor = next.getSupervisor();
                supervisor.killChild(next);
            }
        }

        private void processCtx(final BufferedActorContext bufferedCtx) {
            if (bufferedCtx == null) {
                return;
            }

            final List<Event> events = bufferedCtx.getForwaredEvents();
            final List<Event> continues = bufferedCtx.getContinueEvents();
            final List<Delayed> delayed = bufferedCtx.getDelayedEvents();

            if (!events.isEmpty() || !continues.isEmpty() || !delayed.isEmpty()) {
                PipeLine nextPipe = bufferedCtx.pipeLine;

                if (bufferedCtx.getNewActor() != null) {
                    nextPipe = nextPipe.insert(bufferedCtx.getNewActor()).removeCurrent();
                } else {
                    nextPipe = nextPipe.progress();
                }

                for (final Event nextEvent : events) {
                    final ActorContext nextCtx = ActorContext.withPipeLine(workerThread, system, nextPipe);
                    nextCtx.forward(nextEvent);
                }

                for (final Event downstream : continues) {
                    final DispatchJob job = this.system.createJob(downstream, nextPipe);
                    this.system.dispatchJob(job);
                }

                for (final Delayed delayedMsg : delayed) {
                    final Duration delay = delayedMsg.delay;
                    final Event delayedEvent = delayedMsg.event;
                    // final TimerEvent timerEvent =
                    // TimerEvent.withDelay(delay).withEvent(delayedEvent).build();
                    final DispatchJob job = this.system.createJob(delayedEvent, this.pipeLine);
                    this.system.scheduleJob(delay, job);
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
        private BufferedActorContext invokeActor(final Actor next, final PipeLine pipeLine,
                final Event event) {
            final BufferedActorContext bufferedCtx = new BufferedActorContext(system.getConfig(), workerThread, event.key(), pipeLine);
            try {
                next.onEvent(bufferedCtx, event);
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
        public Scheduler scheduler() {
            throw new RuntimeException("You shouldn't call this method outside of an Actor invocation");
        }

        @Override
        public Configuration getConfig() {
            return system.getConfig();
        }

        @Override
        public void fire(final Event event) {
            throw new RuntimeException("You cannot call fire an event outside of an Actor invocation");
        }

        @Override
        public ActorContext reverse() {
            throw new RuntimeException("You cannot call reverse outside of an Actor invocation");
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
    static class BufferedActorContext implements ActorContext, Scheduler {

        private List<Event> forwardedEvents;

        private List<Event> continueEvents;

        private List<Delayed> delayedEvents;

        /**
         * If the user calls {@link #replace(Actor)} then this is that actor that the user wished to
         * replace the current one with.
         */
        private Actor newActor;

        private final PipeLine pipeLine;

        private boolean killMe = false;

        /**
         * The reversed version of this context. If a user calls ctx.reverse().reverse().reverse()
         * etc we will actually be returning the previous reversed instance. Hence, there can only
         * ever be two contexts present at any given time for this particular context.
         */
        private BufferedActorContext reversedCtx;

        private final int workerThread;

        /**
         * The {@link Key} for the current even that we are working with.
         */
        private final Key currentKey;

        private final Configuration config;

        private BufferedActorContext(final Configuration config, final int workerThread, final Key currentKey, final PipeLine pipeLine) {
            this.config = config;
            this.workerThread = workerThread;
            this.currentKey = currentKey;
            this.pipeLine = pipeLine;
        }

        protected List<Event> getForwaredEvents() {
            if (forwardedEvents != null) {
                return forwardedEvents;
            }
            return Collections.emptyList();
        }

        protected List<Delayed> getDelayedEvents() {
            if (delayedEvents != null) {
                return delayedEvents;
            }
            return Collections.emptyList();
        }

        protected List<Event> getContinueEvents() {
            if (continueEvents != null) {
                return continueEvents;
            }
            return Collections.emptyList();
        }

        protected Actor getNewActor() {
            return this.newActor;
        }

        protected boolean disposeOfActor() {
            // we could have been asked to kill the actor on either
            // the "regular" ctx or the reversed ctx. Either or, the
            // result is the same, kill the actor.
            return this.killMe || this.reversedCtx != null && this.reversedCtx.killMe;
        }

        @Override
        public ActorContext replace(final Actor actor) {
            this.newActor = actor;
            return this;
        }

        @Override
        public void killMe() {
            this.killMe = true;
        }

        @Override
        public Scheduler scheduler() {
            return this;
        }

        @Override
        public Configuration getConfig() {
            return config;
        }

        @Override
        public Cancellable schedule(final Duration delay, final Event event) {
            ensureDelayedEventsList();
            final Delayed delayed = new Delayed(this.workerThread, delay, event);
            this.delayedEvents.add(delayed);
            return new Cancellable() {
                @Override
                public boolean cancel() {
                    return true;
                }
            };
        }

        @Override
        public Cancellable schedule(final Duration delay, final SipTimer timer) {
            final SipTimerEvent timerEvent = SipTimerEvent.withTimer(timer).withKey(this.currentKey).build();
            return schedule(delay, timerEvent);
        }

        @Override
        public void forward(final Event event) {
            if (event == null) {
                return;
            }

            if (forwardedEvents == null) {
                this.forwardedEvents = new ArrayList<>(5);
            }
            this.forwardedEvents.add(event);
        }

        @Override
        public void fire(final Event event) {
            if (continueEvents == null) {
                continueEvents = new ArrayList<>(2);
            }
            this.continueEvents.add(event);
        }

        @Override
        public ActorContext reverse() {
            if (this.reversedCtx == null) {
                final BufferedActorContext reverse =
                        new BufferedActorContext(config, workerThread, currentKey, pipeLine.reverse());
                reverse.reversedCtx = this;
                this.reversedCtx = reverse;
            }
            return this.reversedCtx;
        }

        private void ensureDelayedEventsList() {
            if (this.delayedEvents == null) {
                this.delayedEvents = new ArrayList<>(5);
            }
        }

    }

    /**
     * Simple class to keep the information together.
     */
    public static class Delayed {

        private final Duration delay;
        private final Event event;
        private final int worker;

        private Delayed(final int worker, final Duration delay, final Event msg) {
            this.worker = worker;
            this.delay = delay;
            this.event = msg;
        }
    }


}
