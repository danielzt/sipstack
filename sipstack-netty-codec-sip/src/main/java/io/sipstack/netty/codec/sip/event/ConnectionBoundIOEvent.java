package io.sipstack.netty.codec.sip.event;

import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.impl.IOEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ConnectionBoundIOEvent extends ConnectionIOEvent {
    default boolean isConnectionBoundIOEvent() {
        return true;
    }

    static ConnectionBoundIOEvent create(final Connection connection, final long arrivalTime) {
        return new ConnectionBoundIOEventImpl(connection, arrivalTime);
    }

    class ConnectionBoundIOEventImpl extends IOEventImpl implements ConnectionBoundIOEvent {
        private ConnectionBoundIOEventImpl(final Connection connection, final long arrivalTime) {
            super(connection, arrivalTime);
        }
    }

}
