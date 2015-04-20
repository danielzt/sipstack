/**
 * 
 */
package io.sipstack.application;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.actor.*;
import io.sipstack.core.ApplicationMapper;
import io.sipstack.event.Event;
import io.sipstack.event.SipMsgEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.sipstack.actor.ActorUtils.safePreStart;

/**
 * @author jonas@jonasborjesson.com
 */
public final class ApplicationSupervisor implements Actor, Supervisor {

    private static Logger logger = LoggerFactory.getLogger(ApplicationSupervisor.class);

    private final Map<Key, ApplicationActor> apps = new HashMap<>(100, 0.75f);

    private final ApplicationMapper appMapper;

    private final ActorRef self;

    /**
     * 
     */
    public ApplicationSupervisor(final ActorRef self, final ApplicationMapper mapper) {
        this.self = self;
        this.appMapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void killChild(final Actor actor) {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(final ActorContext ctx, final Event event) {
        final SipMsgEvent sipEvent = (SipMsgEvent) event;
        final SipMessage msg = sipEvent.getSipMessage();

        final Key key = appMapper.map(sipEvent);
        if (self.willExecuteOnSameThread(key)) {
            final ApplicationActor app = ensureApplication(key, sipEvent);
            if (app != null) {
                ctx.replace(app);
            }
            ctx.forward(event);
        } else {
            throw new RuntimeException("haven't implemented this just yet");
        }

        // just consume the ACK
        /*
        if (msg.isAck()) {
            return;
        }

        // for all other requests, just generate a 200 OK response.
        if (msg.isRequest()) {
            final SipResponse response = msg.createResponse(200);
            final Key key = event.key();
            final SipMsgEvent responseEvent = SipMsgEvent.create(key, response);
            ctx.reverse().forward(responseEvent);
        }
        */
    }

    @Override
    public Supervisor getSupervisor() {
        // we are a supervisor so we don't have one ourselves.
        return null;
    }

    private ApplicationActor ensureApplication(final Key key, final SipMsgEvent event) {
        final ApplicationActor app = apps.get(key);
        if (app != null) {
            return app;
        }

        final ApplicationActor newApp = null;
        final Optional<Throwable> exception = safePreStart(newApp);
        if (exception.isPresent()) {
            // TODO: do something about it...
            throw new RuntimeException("The actor threw an exception in PreStart and I havent coded that up yet",
                    exception.get());
        }
        apps.put(key, newApp);
        return newApp;
    }

}
