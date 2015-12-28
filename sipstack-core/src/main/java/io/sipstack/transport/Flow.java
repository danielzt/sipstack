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
 * Also note that this {@link Flow} object is a representation
 * of the underlying flow state machine at a particular point
 * in time. Hence, see this object as a snapshot so if you ask
 * to see the current state of the underlying flow, then that
 * state was correct at the time the snapshot was taken but
 * may not be longer.
 *
 * @author jonas@jonasborjesson.com
 */
public interface Flow {

    /**
     * The difference between a {@link ConnectionId} and a
     *
     * @return
     */
    Optional<ConnectionId> id();

    /**
     * The state of the flow state machine at the time when the {@link io.sipstack.transport.event.FlowEvent}
     * was generated. Hence, you can't hold on to the reference of this {@link Flow} object and expect the
     * state to be true at some later point in time.
     *
     * @return
     */
    FlowState getState();

    /**
     * Kill the flow, which has the effect of purging this flow
     * out of memory. Also, if the underlying transport is TCP based
     * then the connection will also be terminated.
     *
     * This operation is asynchronous and there is a chance that
     * messages are inflight and will still be delivered through
     * this flow.
     *
     * Killing a flow twice has no effect.
     */
    default void kill() {
        // TODO
    }

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
