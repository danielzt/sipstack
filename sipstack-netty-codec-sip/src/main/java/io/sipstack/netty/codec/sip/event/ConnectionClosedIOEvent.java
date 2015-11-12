package io.sipstack.netty.codec.sip.event;

import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.impl.IOEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ConnectionClosedIOEvent extends ConnectionIOEvent {

    default boolean isConnectionClosedIOEvent() {
        return true;
    }

    static ConnectionClosedIOEvent create(final Connection connection, final long arrivalTime) {
        return new ConnectionClosedIOEventImpl(connection, arrivalTime);
    }

    class ConnectionClosedIOEventImpl extends IOEventImpl implements ConnectionClosedIOEvent {
        private ConnectionClosedIOEventImpl(final Connection connection, final long arrivalTime) {
            super(connection, arrivalTime);
        }
    }
}
