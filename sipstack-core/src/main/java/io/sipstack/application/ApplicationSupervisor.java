/**
 * 
 */
package io.sipstack.application;

import io.hektor.core.Actor;
import io.hektor.core.ActorRef;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.core.ApplicationMapper;
import io.sipstack.event.SipMsgEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jonas@jonasborjesson.com
 */
public final class ApplicationSupervisor implements Actor {

    private static Logger logger = LoggerFactory.getLogger(ApplicationSupervisor.class);

    private final ApplicationMapper appMapper;

    private final ActorRef self;

    /**
     * 
     */
    public ApplicationSupervisor(final ActorRef self, final ApplicationMapper mapper) {
        this.self = self;
        this.appMapper = mapper;
    }


    @Override
    public void onReceive(final Object msg) {
        final SipMsgEvent sipEvent = (SipMsgEvent) msg;
        final SipMessage sipMsg = sipEvent.getSipMessage();
        System.err.println("[ApplicationSupervisor] Got msg: " + sipMsg);
    }
}
