package io.sipstack.netty.codec.sip.event;

import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.impl.IOEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ConnectionActiveIOEvent extends ConnectionIOEvent {
    default boolean isConnectionActiveIOEvent() {
        return true;
    }

    static ConnectionActiveIOEvent create(final Connection connection, final long arrivalTime) {
        return new ConnectionActiveIOEventImpl(connection, arrivalTime);
    }

    class ConnectionActiveIOEventImpl extends IOEventImpl implements ConnectionActiveIOEvent {
        private ConnectionActiveIOEventImpl(final Connection connection, final long arrivalTime) {
            super(connection, arrivalTime);
        }
    }
}
