package io.sipstack.transport;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.actor.Actor;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.Key;
import io.sipstack.actor.Supervisor;
import io.sipstack.event.Event;
import io.sipstack.event.IOEvent;
import io.sipstack.event.IOReadEvent;
import io.sipstack.event.SipEvent;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;

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

    @SuppressWarnings("unchecked")
    @Override
    public void onUpstreamEvent(final ActorContext ctx, final Event event) {
        // logger.debug("Processing new event {} ", event.getClass());

        if (event.isIOEvent()) {
            final IOEvent ioEvent = (IOEvent) event;
            if (ioEvent.isSipReadEvent()) {
                final long arrivalTime = event.getArrivalTime();
                final SipMessage msg = ((IOReadEvent<SipMessage>) event).getObject();
                final Key key = Key.withSipMessage(msg);
                final SipEvent sipEvent = SipEvent.create(key, arrivalTime, msg);
                ctx.fireUpstreamEvent(sipEvent);
            }
        }

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

    private static final class DefaultFlow implements Flow {

        @Override
        public ConnectionId getConnectionId() {
            // TODO Auto-generated method stub
            return null;
        }

    }

}
