package io.sipstack.application;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.ActorSupport;
import io.sipstack.event.Event;
import io.sipstack.event.IOEvent;
import io.sipstack.event.IOWriteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * @author jonas@jonasborjesson.com
 */
public class ApplicationActor extends ActorSupport<Event,ApplicationState> {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationActor.class);

    public ApplicationActor() {
        super("asdf", ApplicationState.INIT, ApplicationState.values());

        when(ApplicationState.INIT, init);
    }

    private final Consumer<Event> init = event -> {
        final IOEvent<SipMessage>  ioEvent = event.toSipIOEvent();
        final SipMessage msg = ioEvent.getObject();
        if (!msg.isAck()) {
            final SipResponse response = msg.createResponse(200);
            sender().tell(IOWriteEvent.create(response), self());
        }
    };

    @Override
    protected Logger logger() {
        return logger;
    }
}
