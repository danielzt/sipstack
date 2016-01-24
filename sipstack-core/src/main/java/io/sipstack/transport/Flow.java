package io.sipstack.transport;

import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.Transport;
import io.sipstack.netty.codec.sip.ConnectionId;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
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
    ConnectionId id();

    default Transport getTransport() {
        return id().getProtocol();
    }

    default void send(SipMessage msg) {
        throw new IllegalStateException("This flow is not connected");
    }

    default void send(SipMessage.Builder msg) {
        throw new IllegalStateException("This flow is not connected");
    }

    /**
     * The state of the flow state machine at the time when the {@link io.sipstack.transport.event.FlowEvent}
     * was generated. Hence, you can't hold on to the reference of this {@link Flow} object and expect the
     * state to be true at some later point in time.
     *
     * @return
     */
    FlowState getState();

    /**
     * Get the local port to which this {@link Flow} is listening to.
     *
     * @return
     */
    default int getLocalPort() {
        return id().getLocalPort();
    }

    /**
     * Get the local ip-address to which this {@link Connection} is listening to
     * as a byte-array.
     *
     * @return
     */
    default byte[] getRawLocalIpAddress() {
        return id().getRawLocalIpAddress();
    }

    /**
     * Get the local ip-address to which this {@link Connection} is listening to
     * as a {@link String}.
     *
     * @return
     */
    default String getLocalIpAddress() {
        return id().getLocalIpAddress();
    }

    default InetSocketAddress getLocalAddress() {
        return id().getLocalAddress();
    }

    default Buffer getLocalIpAddressAsBuffer() {
        return Buffers.wrap(id().getLocalIpAddress());
    }

    /**
     * Get the remote address to which this {@link Flow} is connected to.
     *
     * @return
     */
    default InetSocketAddress getRemoteAddress() {
        return id().getRemoteAddress();
    }

    /**
     * Get the remote port to which this {@link Flow} is connected to.
     *
     * @return
     */
    default int getRemotePort() {
        return id().getRemotePort();
    }

    /**
     * Get the remote ip-address to which this {@link Connection} is connected
     * to as a byte-array.
     *
     * @return
     */
    default byte[] getRawRemoteIpAddress() {
        return id().getRawRemoteIpAddress();
    }

    /**
     * Get the remote ip-address to which this {@link Connection} is connected
     * to as a {@link String}.
     *
     * @return
     */
    default String getRemoteIpAddress() {
        return id().getRemoteIpAddress();
    }

    default Buffer getRemoteIpAddressAsBuffer() {
        return Buffers.wrap(id().getRemoteIpAddress());
    }

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

    /**
     * If this {@link Flow} has failed then it typically has done
     * so because of a failure to connect to the remote host, in which
     * case the exception can be retrieved through {@link Flow#getFailureCause()}.
     *
     * @return true if this flow has failed.
     */
    default boolean isFailed() {
        return !isValid();
    }

    /**
     * Check if the {@link Flow} was cancelled before it was successfully established.
     * This is how you check if a flow failed due to an exception or because it
     * was cancelled since in both cases, the flow will be marked as failed.
     *
     * Also note that for a cancelled flow, the {@link Flow#getFailureCause()} will
     * return a {@link CancellationException}.
     *
     * @return true if this flow was cancelled, false otherwise.
     */
    default boolean isCancelled() {
        return false;
    }

    /**
     * If this {@link Flow} has failed then it may have failed due
     * to some exception, which can be retrieved through this method.
     *
     * @return the cause if there is one (wrapped in an {@link Optional})
     */
    default Optional<Throwable> getFailureCause() {
        return Optional.empty();
    }

    interface Builder {

        /**
         * Specify the port to connect to.
         *
         * @param port
         * @return
         * @throws IllegalArgumentException in case you already have specified
         * the remote address when creating this builder via the
         * {@link TransportLayer#createFlow(InetSocketAddress)} method.
         */
        Builder withPort(int port) throws IllegalArgumentException;

        Builder withTransport(Transport transport);

        /**
         * In a multi-homed environment, you may want to specify which network interface
         * you wish to connect through. If not specified, the default network interface
         * will be used.
         *
         * @param interfaceName
         * @return
         */
        Builder withNetworkInterface(String interfaceName);

        /**
         *
         * @param consumer
         * @return
         */
        Builder onSuccess(Consumer<Flow> consumer);

        /**
         * If we fail to create the flow, typically because we were unable to
         * connect to the remote host, or failed to perform a DNS lookup, then
         * this function will be called. The {@link Flow} will be invalid (which
         * you can check through {@link Flow#isValid()} or {@link Flow#isFailed()})
         * and the exception that caused us to fail is retrievable through
         * {@link Flow#fail}
         *
         * Note, in regards to the DNS lookup the {@link Flow} performs: a flow will
         * not perform a full RFC3263 recommended flow, that you, as the user of the flow,
         * is responsible for. Instead, because the flow will create a {@link InetSocketAddress},
         * the flow will resolve the host using regular A lookup, which of course can fail.
         * The recommended way to use a {@link Flow} is to only ask it to connect to an already
         * resolved address, i.e., only use IP-addresses since you don't want the {@link InetSocketAddress}
         * to do a DNS lookup for you since it is asynchronous. Plus, if that resolves to many
         * IP-addresses, the flow will not really deal with it. Only one will try and you may
         * want to try to establish the flow to another IP-address, as returned
         * by your NAPTR/SRV/A lookups.
         *
         * @param consumer
         * @return
         */
        Builder onFailure(Consumer<Flow> consumer);

        /**
         * If the attempt to establish the flow is cancelled before we were
         * able to do so then this function will be called.
         *
         * The {@link Flow} as passed into this function will be marked as failed
         * and the {@link Flow#getFailureCause()} will return a {@link CancellationException}
         * whose {@link CancellationException#getMessage()} will return the reason for
         * the cancellation (most likely just a generic string, which will not say much. Note,
         * that string is of course for human consumption. Do not base any logic off of it!)
         *
         * @param consumer
         * @return
         */
        Builder onCancelled(Consumer<Flow> consumer);

        /**
         *
         * @return
         * @throws IllegalArgumentException in case any of the supplied arguments are wrong
         * or some of the mandatory arguments are missing.
         */
        CompletableFuture<Flow> connect() throws IllegalArgumentException;
    }
}
