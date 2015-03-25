/**
 * 
 */
package io.sipstack.transport;

import static io.sipstack.actor.ActorUtils.safePreStart;
import io.sipstack.actor.Actor;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.ActorRef;
import io.sipstack.actor.Supervisor;
import io.sipstack.event.Event;
import io.sipstack.event.IOEvent;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The {@link TransportSupervisor} is responsible for creating and maintaining
 * 
 * @author jonas@jonasborjesson.com
 */
public class TransportSupervisor implements Actor, Supervisor {

    private final Map<ConnectionId, FlowActor> flows = new HashMap<>(100, 0.75f);

    private final ActorRef self;

    /**
     * 
     */
    public TransportSupervisor(final ActorRef self) {
        this.self = self;
    }

    @Override
    public void killChild(final Actor actor) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onEvent(final ActorContext ctx, final Event event) {
        try {
            final IOEvent ioEvent = event.toIOEvent();
            final FlowActor flow = ensureFlow(ioEvent.getConnection());
            ctx.replace(flow);
            ctx.forward(event);
        } catch (final ClassCastException e) {
            // no???
            e.printStackTrace();
            throw e;
        }
    }

    private FlowActor ensureFlow(final Connection connection) {
        final ConnectionId id = connection.id();
        final FlowActor flow = this.flows.get(id);
        if (flow != null) {
            return flow;
        }

        final FlowActor newFlow = FlowActor.create(this, connection);
        final Optional<Throwable> exception = safePreStart(newFlow);
        if (exception.isPresent()) {
            throw new RuntimeException("The actor threw an exception in PostStop and I havent coded that up yet",
                    exception.get());
        }

        this.flows.put(id, newFlow);
        return newFlow;
    }

    @Override
    public ActorRef self() {
        return this.self;
    }


}
