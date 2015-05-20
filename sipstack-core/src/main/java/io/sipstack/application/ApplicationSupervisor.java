/**
 * 
 */
package io.sipstack.application;

import io.hektor.core.Actor;
import io.hektor.core.ActorRef;
import io.hektor.core.Props;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.event.Event;
import io.sipstack.event.InitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public final class ApplicationSupervisor implements Actor {

    private static Logger logger = LoggerFactory.getLogger(ApplicationSupervisor.class);

    /**
     * Whenever we process a new downstream event, which typically will be
     * a new SIP request, we have to establish a chain for that request
     * and we will do so by sending it our downstream actor, which typically
     * will be a dialog supervisor or a transaction supervisor depending on
     * the configuration of the SIP stack.
     */
    private ActorRef downstreamActor;

    @Override
    public void onReceive(final Object msg) {

        final Event event = (Event)msg;
        if (event.isSipIOEvent()) {
            final SipMessage sip = event.toSipIOEvent().getObject();
            System.err.println("[ApplicationSupervisor] Got msg: " + sip);
            final String appId = getApplicationIdentifier(sip);

            final Optional<ActorRef> child = ctx().child(appId);
            final ActorRef app = child.orElseGet(() -> {

                // TODO: we need to know how to create the application.
                // need to figure out the best way of doing this.
                final Props props = Props.forActor(ApplicationActor.class).build();
                return ctx().actorOf(appId, props);
            });

            // forward the message
            app.tell(event, sender());

        } else if (event.isInitEvent()) {
            System.err.println("init event!!!");
            downstreamActor = ((InitEvent)event).downstreamSupervisor;
        } else {
        }
    }

    /**
     *
     * @param msg
     * @return
     */
    private String getApplicationIdentifier(final SipMessage msg) {
        return msg.getCallIDHeader().toString();
    }

}
