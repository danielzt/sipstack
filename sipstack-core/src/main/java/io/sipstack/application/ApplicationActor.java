/**
 * 
 */
package io.sipstack.application;

import io.sipstack.actor.ActorBase;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.ActorRef;
import io.sipstack.actor.Supervisor;
import io.sipstack.event.Event;
import org.slf4j.Logger;

import java.util.function.BiConsumer;

/**
 * @author jonas@jonasborjesson.com
 */
public class ApplicationActor extends ActorBase<ApplicationState> {

    /**
     * 
     */
    public ApplicationActor() {
        super("asdf", ApplicationState.INIT, ApplicationState.values());

        when(ApplicationState.INIT, init);
        onEnter(ApplicationState.INIT, onEnterInit);
    }

    private final BiConsumer<ActorContext, Event> onEnterInit = (ctx, event) -> {
        System.err.println("on enter init");
    };

    private final BiConsumer<ActorContext, Event> init = (ctx, event) -> {
        System.err.println("on init");
    };

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

    @Override
    protected Logger logger() {
        return null;
    }

}
