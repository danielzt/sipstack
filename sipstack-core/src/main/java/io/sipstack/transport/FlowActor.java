package io.sipstack.transport;

import io.hektor.core.Actor;
import io.hektor.core.ActorRef;
import io.hektor.core.Cancellable;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.event.Event;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.netty.codec.sip.config.FlowConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowActor implements Actor {

    private static final Logger logger = LoggerFactory.getLogger(FlowActor.class);

    /**
     * The next actor in the chain, which typically will be a TransactionSupervisor
     * behind a router.
     */
    private final ActorRef upstream;

    private final Connection connection;

    /**
     * A flow will only stay open for so long before shutting down, tearing down
     * the connection with it. This timer keeps track of that and is configurable.
     * It is not a very accurate timer the way this has been implemented, which
     * is on purpose. If you set the timeout to 1 min, there is a good chance
     * the timeout will actually not happen until 2 min. The reason is that
     * we don't cancel this timer as soon as we see a new even that would
     * have reset the timer but rather we check how long ago it was when
     * we last got an event across this flow when the timer fires. Hence,
     * if the timer fires and it was 59 seconds since the last message, we will
     * not kill the actor but re-schedule the timer, which means that next
     * time it fires it will now have been 1 min 59 seconds since the last
     * message and at that time we will kill off the flow.
     */
    private Cancellable timer;

    private final FlowConfiguration config;

    public FlowActor(final ActorRef upstream, final Connection connection, final FlowConfiguration config) {
        this.upstream = upstream;
        this.connection = connection;
        this.config = config;
    }

    @Override
    public void onReceive(final Object msg) {
        final Event event = (Event)msg;
        if (event.isSipReadEvent()) {
            upstream.tell(msg, self());
        } else if (event.isSipWriteEvent()) {
            final SipMessage sip = event.toSipIOEvent().getObject();
            connection.send(sip);
        }
    }

    /**
     * Just a temp method for acting as a UAS. Only for testing.
     * @param msg
     */
    private void actUAS(final SipMessage msg) {
        if (!msg.isAck()) {
            connection.send(msg.createResponse(200));
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
