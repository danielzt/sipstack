/**
 * 
 */
package io.sipstack.transport;

import io.sipstack.actor.Actor;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.Supervisor;
import io.sipstack.event.Event;
import io.sipstack.event.IOEvent;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link TransportSupervisor} is responsible for creating and maintaining
 * 
 * @author jonas@jonasborjesson.com
 */
public class TransportSupervisor implements Actor, Supervisor {

    private final Map<ConnectionId, FlowActor> flows = new HashMap<>(100, 0.75f);

    /**
     * 
     */
    public TransportSupervisor() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void killChild(final Actor actor) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onUpstreamEvent(final ActorContext ctx, final Event event) {
        try {
            final IOEvent ioEvent = (IOEvent) event;
            final FlowActor flow = ensureFlow(ioEvent.getConnection());
            ctx.replace(flow);
            ctx.forwardUpstreamEvent(event);
        } catch (final ClassCastException e) {
            // no???
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void onDownstreamEvent(final ActorContext ctx, final Event event) {
        throw new RuntimeException("Not implemented just yet");
    }

    private FlowActor ensureFlow(final Connection connection) {
        final ConnectionId id = connection.id();
        final FlowActor flow = this.flows.get(id);
        if (flow != null) {
            return flow;
        }

        final FlowActor newFlow = FlowActor.create(this, connection);
        this.flows.put(id, newFlow);
        return newFlow;
    }


}
