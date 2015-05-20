package io.sipstack.application;

import io.sipstack.actor.ActorSupport;
import io.sipstack.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jonas@jonasborjesson.com
 */
public class ApplicationActor extends ActorSupport<Event,ApplicationState> {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationActor.class);

    public ApplicationActor() {
        super("asdf", ApplicationState.INIT, ApplicationState.values());

    }
    @Override
    protected Logger logger() {
        return logger;
    }
}
