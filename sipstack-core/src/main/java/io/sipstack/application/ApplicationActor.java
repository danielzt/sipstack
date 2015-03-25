/**
 * 
 */
package io.sipstack.application;

import io.sipstack.actor.Actor;
import io.sipstack.actor.ActorRef;
import io.sipstack.actor.Supervisor;

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
    public Supervisor getSupervisor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ActorRef self() {
        // TODO Auto-generated method stub
        return null;
    }

}
