/**
 * 
 */
package io.sipstack.application;

import io.sipstack.actor.Actor;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.Supervisor;
import io.sipstack.event.Event;

/**
 * @author jonas
 *
 */
public final class ApplicationSupervisor implements Actor, Supervisor {

    /**
     * 
     */
    public ApplicationSupervisor() {
        // TODO Auto-generated constructor stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void killChild(Actor actor) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpstreamEvent(ActorContext ctx, Event event) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDownstreamEvent(ActorContext ctx, Event event) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Supervisor getSupervisor() {
        // TODO Auto-generated method stub
        return null;
    }

}
