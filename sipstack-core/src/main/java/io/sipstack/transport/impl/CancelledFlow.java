package io.sipstack.transport.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.ConnectionId;

/**
 * @author jonas@jonasborjesson.com
 */
public class CancelledFlow implements InternalFlow {

    @Override
    public ConnectionId id() {
        return null;
    }

    @Override
    public void write(SipMessage msg) {

    }
}
