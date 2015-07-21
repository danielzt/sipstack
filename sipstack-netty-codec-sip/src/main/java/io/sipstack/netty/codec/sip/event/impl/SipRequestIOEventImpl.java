package io.sipstack.netty.codec.sip.event.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.SipRequestIOEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipRequestIOEventImpl extends SipMessageIOEventImpl implements SipRequestIOEvent {

    /**
     * @param connection
     * @param msg
     * @param arrivalTime
     */
    public SipRequestIOEventImpl(final Connection connection, final SipMessage request, long arrivalTime) {
        super(connection, request, arrivalTime);
    }
}
