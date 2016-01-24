package io.sipstack.transport.impl;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.Transport;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowState;
import io.sipstack.transport.event.FlowEvent;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public abstract class InternalFlow implements Flow {

    private final Optional<Connection> connection;

    private final ConnectionId id;

    private final FlowState state;

    protected InternalFlow(final ConnectionId id, final Optional<Connection> connection, final FlowState state) {
        this.id = id;
        this.connection = connection;
        this.state = state;
    }

    public Optional<Connection> connection() {
        return connection;
    }

    public FlowState getState() {
        return state;
    }

    @Override
    public ConnectionId id() {
        return id;
    }

    public void send(final SipMessage msg) {
        connection.orElseThrow(() -> new IllegalStateException("This flow is not connected")).send(FlowEvent.create(this, msg));
    }

    public void send(final SipMessage.Builder msg) {
        connection.orElseThrow(() -> new IllegalStateException("This flow is not connected")).send(FlowEvent.create(this, msg));
    }

    @Override
    public Transport getTransport() {
        return connection.orElseThrow(() -> new IllegalStateException("This flow is not connected")).getTransport();
    }

    @Override
    public int getLocalPort() {
        return connection.orElseThrow(() -> new IllegalStateException("This flow is not connected")).getLocalPort();
    }

    @Override
    public byte[] getRawLocalIpAddress() {
        return connection.orElseThrow(() -> new IllegalStateException("This flow is not connected")).getRawLocalIpAddress();
    }

    @Override
    public String getLocalIpAddress() {
        return connection.orElseThrow(() -> new IllegalStateException("This flow is not connected")).getLocalIpAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return connection.orElseThrow(() -> new IllegalStateException("This flow is not connected")).getLocalAddress();
    }

    @Override
    public Buffer getLocalIpAddressAsBuffer() {
        return connection.orElseThrow(() -> new IllegalStateException("This flow is not connected")).getLocalIpAddressAsBuffer();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return connection.orElseThrow(() -> new IllegalStateException("This flow is not connected")).getRemoteAddress();
    }

    @Override
    public int getRemotePort() {
        return connection.orElseThrow(() -> new IllegalStateException("This flow is not connected")).getRemotePort();
    }

    @Override
    public byte[] getRawRemoteIpAddress() {
        return connection.orElseThrow(() -> new IllegalStateException("This flow is not connected")).getRawRemoteIpAddress();
    }

    @Override
    public String getRemoteIpAddress() {
        return connection.orElseThrow(() -> new IllegalStateException("This flow is not connected")).getRemoteIpAddress();
    }

    @Override
    public Buffer getRemoteIpAddressAsBuffer() {
        return connection.orElseThrow(() -> new IllegalStateException("This flow is not connected")).getRemoteIpAddressAsBuffer();
    }

}
