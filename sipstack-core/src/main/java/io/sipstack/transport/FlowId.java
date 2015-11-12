package io.sipstack.transport;

import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.netty.codec.sip.ConnectionId;

/**
 * The only difference between a {@link FlowId} and a {@link io.sipstack.netty.codec.sip.ConnectionId} is that the
 * flow id is encrypted in order to prevent tampering. Imagine the following scenario:
 *
 * An attacker is maliciously changing the connection id on the Route header to point
 * to localhost:5060. When the stack is about to send out the message it will check the Route header
 * and determine that there is a connection id encoded on the URL and it will therefore use that
 * information to lookup a connection and send the message there, potentially creating a loop
 * in the stack.
 *
 * So, by encrypting the connection id (which now is called a flow id) by a key only known
 * to the server itself then it can detect whether or not someone has been trying to
 * tamper with it and if so, reject the request in some fashion (silently drop, generate a 403
 * or whatever)
 *
 * @author jonas@jonasborjesson.com
 */
public interface FlowId {

    /**
     * Create a new {@link FlowId}.
     * @param id
     * @return
     * @throws IllegalArgumentException in case the connection id is null
     */
    static FlowId create(final ConnectionId id) throws IllegalArgumentException {
        PreConditions.ensureNotNull(id, "The Connection ID cannot be null");
        return new BasicFlowId(id);
    }

    class BasicFlowId implements FlowId {

        private final ConnectionId id;

        private BasicFlowId(final ConnectionId id) {
            this.id = id;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o ==  null) {
                return false;
            }

            try {
                final BasicFlowId that = (BasicFlowId) o;
                return id.equals(that.id);
            } catch (final ClassCastException e) {
                return false;
            }

        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

}
