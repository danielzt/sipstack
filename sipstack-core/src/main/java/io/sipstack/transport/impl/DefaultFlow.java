package io.sipstack.transport.impl;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.transport.FlowState;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultFlow extends InternalFlow {

    public DefaultFlow(final Connection connection, final FlowState state) {
        super(connection.id(), Optional.of(connection), state);
    }


}
