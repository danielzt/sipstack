package io.sipstack.netty.codec.sip.event;

import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.impl.IOEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ConnectionOpenedIOEvent extends ConnectionIOEvent {

    default boolean isConnectionOpenedIOEvent() {
        return true;
    }

    static ConnectionOpenedIOEvent create(final Connection connection, final long arrivalTime) {
        return new ConnectionOpenedIOEventImpl(connection, arrivalTime);
    }

    class ConnectionOpenedIOEventImpl extends IOEventImpl implements ConnectionOpenedIOEvent {
        private ConnectionOpenedIOEventImpl(final Connection connection, final long arrivalTime) {
            super(connection, arrivalTime);
        }
    }
}
