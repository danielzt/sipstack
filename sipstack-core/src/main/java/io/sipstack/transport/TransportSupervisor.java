/**
 * 
 */
package io.sipstack.transport;

import io.hektor.core.Actor;
import io.hektor.core.ActorRef;
import io.hektor.core.Props;
import io.sipstack.event.IOReadEvent;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.netty.codec.sip.SipMessageEvent;

import java.util.Optional;

/**
 * The {@link TransportSupervisor} is responsible for creating and maintaining
 * 
 * @author jonas@jonasborjesson.com
 */
public class TransportSupervisor implements Actor {

    /**
     * 
     */
    public TransportSupervisor() {
    }

    /*
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
    */

    @Override
    public void onReceive(final Object msg) {
        if (SipMessageEvent.class.isAssignableFrom(msg.getClass())) {
            final SipMessageEvent sipEvent = (SipMessageEvent)msg;
            final ConnectionId id = sipEvent.getConnection().id();
            final String idStr = id.encodeAsString();
            final Optional<ActorRef> child = ctx().child(idStr);
            final ActorRef flow = child.orElseGet(() ->  {
                final Props props = Props.forActor(FlowActor.class)
                        .withConstructorArg(sipEvent.getConnection())
                        .build();
                return ctx().actorOf(idStr, props);
            });

            flow.tell(IOReadEvent.create(sipEvent), self());

            // System.err.println("[" + Thread.currentThread().getName() + "] [TransportSupervisor] onRecieve");
            System.err.println(Thread.currentThread().getName() + " " + this);
        } else {
            System.err.println("[TransportSupervisor] No clue what I got!!! ");
        }
    }

}
