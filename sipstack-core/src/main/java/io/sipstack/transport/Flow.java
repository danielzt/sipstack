package io.sipstack.transport;

import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.netty.codec.sip.Transport;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A flow is a bidirectional communication path between two endpoints.
 * A flow has to be maintained so that NAT/FW doesn't close down the
 * path between the two endpoints and is done by issuing keep-alive
 * traffic. The endpoint that initiated the flow is typically responsible
 * for maintaining the flow, i.e., it is its responsibility to issue
 * some sort of "ping" and the other end will respond with a pong.
 * However, there are situations where the flow initiator (the client)
 * is unable to fulfill this requirement so thererefore any side has
 * to be prepared to take on the role of being the client.
 *
 * The basic idea of a flow is separated from the notion of what
 * transport protocol is used but is typically UPD or TCP and will
 * affect how the flow is maintained. E.g., if TCP is used, a flow is
 * equal to a TCP connection and commonly, double CRLF is used as a
 * ping and a single CRLF as pong. For UDP, however, RFC5626 recommends
 * to use stun but this implementation can be configured to use a
 * variety of strategies for issuing and/or responding to a ping.
 *
 * @author jonas@jonasborjesson.com
 */
public interface Flow {

    /**
     * The difference between a {@link ConnectionId} and a
     * @return
     */
    Optional<ConnectionId> id();

    default boolean isValid() {
        return true;
    }

    default boolean isFailed() {
        return !isValid();
    }

    default String failedReason() {
        return "Not implemented right now";
    }

    interface Builder {

        Builder withPort(int port);

        Builder withTransport(Transport transport);

        Builder onSuccess(Consumer<Flow> consumer);

        Builder onFailure(Consumer<Flow> consumer);

        Builder onCancelled(Consumer<Flow> consumer);

        /**
         *
         * @return
         * @throws IllegalArgumentException in case any of the supplied arguments are wrong
         * or some of the mandatory arguments are missing.
         */
        FlowFuture connect() throws IllegalArgumentException;
    }
}
