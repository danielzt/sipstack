package io.sipstack.netty.codec.sip.event.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.SipResponseIOEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipResponseIOEventImpl extends SipMessageIOEventImpl implements SipResponseIOEvent{

    /**
     * @param connection
     * @param msg
     * @param arrivalTime
     */
    public SipResponseIOEventImpl(final Connection connection, final SipMessage response, long arrivalTime) {
        super(connection, response, arrivalTime);
    }
}
