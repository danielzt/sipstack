package io.sipstack.transport.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultFlow extends InternalFlow {

    public DefaultFlow(final Connection connection) {
        super(Optional.of(connection));
    }


}
