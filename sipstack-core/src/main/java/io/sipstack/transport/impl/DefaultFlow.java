package io.sipstack.transport.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.transport.Flow;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultFlow implements Flow {

    private final Connection connection;

    public DefaultFlow(final Connection connection) {
        this.connection = connection;
    }

    public Connection connection() {
        return connection;
    }

    @Override
    public ConnectionId id() {
        return connection.id();
    }

    @Override
    public void write(final SipMessage msg) {
        connection.send(msg);
    }
}
