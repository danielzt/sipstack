package io.sipstack.transport;

import io.pkts.packet.sip.SipMessage;

import java.net.InetSocketAddress;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransportLayer {

    /**
     * Send a message and let the transport layer figure out the address, and which {@link Flow} to use, by following
     * the procedures of RFC 3263. Which flow that was ultimately selected is conveyed via the method
     * {@link TransportUser#onWriteCompleted(Flow, SipMessage)} or if it failed the method
     * {@link TransportUser#onWriteFailed(Flow, SipMessage)} will be called.
     *
     * @param msg
     */
    // void write(SipMessage msg);

    /**
     * Force the {@link SipMessage} to be sent across the specified {@link Flow}. If the flow
     * is "down" an attempt to re-establish the same flow will be made.
     *
     * When the write has been completed the {@link TransportUser#onWriteCompleted(Flow, SipMessage)} or
     * {@link TransportUser#onWriteFailed(Flow, SipMessage)} will be called, which then will convey which
     * flow was ultimately used.
     *
     * @param flow
     * @param msg
     */
    // void write(Flow flow, SipMessage msg);

    /**
     *
     * @param host
     * @return
     * @throws IllegalArgumentException
     */
    Flow.Builder createFlow(String host) throws IllegalArgumentException;

    Flow.Builder createFlow(InetSocketAddress remoteHost) throws IllegalArgumentException;


}
