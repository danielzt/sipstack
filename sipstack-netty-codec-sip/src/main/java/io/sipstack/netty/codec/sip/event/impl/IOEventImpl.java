package io.sipstack.netty.codec.sip.event.impl;

import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.IOEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class IOEventImpl implements IOEvent {

    private final Connection connection;
    private final long arrivalTime;

    public IOEventImpl(final Connection connection, final long arrivalTime) {
        this.connection = connection;
        this.arrivalTime = arrivalTime;
    }

    @Override
    public final Connection connection() {
        return connection;
    }

    @Override
    public long arrivalTime() {
        return arrivalTime;
    }
}
