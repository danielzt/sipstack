package io.sipstack.netty.codec.sip.actor;

import io.sipstack.netty.codec.sip.event.Event;

import java.util.Optional;

/**
 * For many of our "actors", such as the {@link InviteServerTransaction}
 * it will only ever produce at most three things. Zero or one upstream event, zero or one
 * downstream event and zero or one timer event. Therefore, this context only allows that
 * you do just that, i.e., only a single event of each kind.
 *
 * @author jonas@jonasborjesson.com
 */
public class SingleContext implements ActorContext {

    private Optional<Event> upstream = Optional.empty();

    private Optional<Event> downstream = Optional.empty();

    private final Scheduler scheduler;

    public SingleContext(final Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Scheduler scheduler() {
        return this.scheduler;
    }

    @Override
    public void forwardUpstream(final Event event) {
        if (upstream.isPresent()) {
            throw new IllegalStateException("An upstream event has already been forwarded");
        }

        upstream = Optional.ofNullable(event);
    }

    @Override
    public void forwardDownstream(final Event event) {
        if (downstream.isPresent()) {
            throw new IllegalStateException("A downstream event has already been forwarded");
        }

        downstream = Optional.ofNullable(event);
    }

    public Optional<Event> upstream() {
        return upstream;
    }

    public Optional<Event> downstream() {
        return downstream;
    }
}
