package io.sipstack.transport.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.transport.Flow;

/**
 * @author jonas@jonasborjesson.com
 */
public class CancelledFlow implements Flow {

    @Override
    public ConnectionId id() {
        return null;
    }

    @Override
    public void write(SipMessage msg) {

    }
}
