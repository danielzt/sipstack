package io.sipstack.transport.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.transport.Flow;

/**
 * @author jonas@jonasborjesson.com
 */
public class FailureFlow implements Flow {


    private final Throwable cause;

    public FailureFlow(final Throwable cause) {
        this.cause = cause;
    }

    @Override
    public ConnectionId id() {
        return null;
    }

    @Override
    public void write(SipMessage msg) {
        // TODO: may want to throw something more specif, FlowFailureException?
        throw new RuntimeException("This flow is a failure! Can't write to it!");
    }
}
