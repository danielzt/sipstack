package io.sipstack.netty.codec.sip.event.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.SipResponseBuilderIOEvent;
import io.sipstack.netty.codec.sip.event.impl.IOEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipResponseBuilderIOEventImpl extends IOEventImpl implements SipResponseBuilderIOEvent {

    private final SipMessage.Builder<SipResponse> builder;

    public SipResponseBuilderIOEventImpl(final Connection connection,
                                        final long arrivalTime,
                                        final SipMessage.Builder<SipResponse> builder) {
        super(connection, arrivalTime);
        this.builder = builder;
    }

    @Override
    public SipMessage.Builder<SipResponse> getBuilder() {
        return builder;
    }
}
