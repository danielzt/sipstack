package io.sipstack.transport;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.actor.Actor;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.Supervisor;
import io.sipstack.event.Event;
import io.sipstack.event.SipEvent;
import io.sipstack.netty.codec.sip.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowActor implements Actor {

    private static final Logger logger = LoggerFactory.getLogger(FlowActor.class);

    private final Connection connection;
    private final TransportSupervisor supervisor;

    public static FlowActor create(final TransportSupervisor parent, final Connection connection) {
        return new FlowActor(parent, connection);
    }

    private FlowActor(final TransportSupervisor supervisor, final Connection connection) {
        this.supervisor = supervisor;
        this.connection = connection;
    }

    @Override
    public void onUpstreamEvent(final ActorContext ctx, final Event event) {
        logger.debug("Processing new event {} ", event.getClass());

        // swap to a different thread (potentially anyway)
        ctx.continueUpstreamEvent(event);
    }

    @Override
    public void onDownstreamEvent(final ActorContext ctx, final Event event) {
        if (event instanceof SipEvent) {
            final SipMessage msg = ((SipEvent) event).getSipMessage();
            this.connection.send(msg);
        }
    }

    @Override
    public Supervisor getSupervisor() {
        return this.supervisor;
    }

}
