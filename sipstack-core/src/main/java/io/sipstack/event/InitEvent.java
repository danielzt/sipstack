package io.sipstack.event;

import io.hektor.core.ActorRef;

/**
 * Simple initializing event send to the xxx supervisor
 * after they have been created.
 *
 * @author jonas@jonasborjesson.com
 */
public final class InitEvent extends AbstractEvent {

    public final ActorRef downstreamSupervisor;
    public final ActorRef upstreamSupervisor;

    public InitEvent(final ActorRef downstreamSupervisor, final ActorRef upstreamSupervisor) {
        super(System.currentTimeMillis());
        this.downstreamSupervisor = downstreamSupervisor;
        this.upstreamSupervisor = upstreamSupervisor;
    }

    @Override
    public boolean isInitEvent() {
        return true;
    }
}
