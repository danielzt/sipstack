/**
 * 
 */
package io.sipstack.application;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.Actor;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.Key;
import io.sipstack.actor.Supervisor;
import io.sipstack.event.Event;
import io.sipstack.event.SipMsgEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jonas@jonasborjesson.com
 */
public final class ApplicationSupervisor implements Actor, Supervisor {

    private static Logger logger = LoggerFactory.getLogger(ApplicationSupervisor.class);

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
        // just consume the ACK
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
    }

    @Override
    public Supervisor getSupervisor() {
        // TODO Auto-generated method stub
        return null;
    }

}
