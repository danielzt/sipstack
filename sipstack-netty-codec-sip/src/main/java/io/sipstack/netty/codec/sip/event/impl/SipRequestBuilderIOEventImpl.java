package io.sipstack.netty.codec.sip.event.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.SipRequestBuilderIOEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipRequestBuilderIOEventImpl extends IOEventImpl implements SipRequestBuilderIOEvent {

    private final SipMessage.Builder<SipRequest> builder;

    public SipRequestBuilderIOEventImpl(final Connection connection,
                                        final long arrivalTime,
                                        final SipMessage.Builder<SipRequest> builder) {
        super(connection, arrivalTime);
        this.builder = builder;
    }

    @Override
    public SipMessage.Builder<SipRequest> getBuilder() {
        return builder;
    }
}
