package io.sipstack.transport;

import io.hektor.core.Actor;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.event.Event;
import io.sipstack.event.IOEvent;
import io.sipstack.event.IOReadEvent;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowActor implements Actor {

    private static final Logger logger = LoggerFactory.getLogger(FlowActor.class);

    private final Connection connection;

    public FlowActor(final Connection connection) {
        this.connection = connection;
    }

    @Override
    public void onReceive(final Object msg) {
        final Event event = (Event)msg;
        if (event.isIOEvent()) {
            final IOEvent ioEvent = (IOEvent) event;
            if (ioEvent.isSipReadEvent()) {
                final long arrivalTime = event.getArrivalTime();
                final SipMessage sipMsg = ((IOReadEvent<SipMessage>) event).getObject();
                // final Key key = Key.withSipMessage(sipMsg);
                // final SipMsgEvent sipEvent = SipMsgEvent.create(key, arrivalTime, sipMsg);
            }
        } else if (event.isSipMsgEvent()) {
            // final SipMessage msg = event.toSipMsgEvent().getSipMessage();
            // this.connection.send(msg);
        }

    }

    private static final class DefaultFlow implements Flow {

        @Override
        public ConnectionId getConnectionId() {
            // TODO Auto-generated method stub
            return null;
        }

    }

}
