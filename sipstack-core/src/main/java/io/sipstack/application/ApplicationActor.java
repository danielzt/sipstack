/**
 * 
 */
package io.sipstack.application;

import io.sipstack.actor.Actor;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.Supervisor;
import io.sipstack.event.Event;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public class ApplicationActor implements Actor {

    /**
     * 
     */
    public ApplicationActor() {
        // TODO Auto-generated constructor stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpstreamEvent(final ActorContext ctx, final Event event) {
        // TODO Auto-generated method stub
        System.err.println("Guess I should pass it on to the actual application");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDownstreamEvent(final ActorContext ctx, final Event event) {
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
