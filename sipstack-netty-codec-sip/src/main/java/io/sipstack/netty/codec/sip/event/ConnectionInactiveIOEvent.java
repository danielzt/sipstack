package io.sipstack.netty.codec.sip.event;

import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.impl.IOEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ConnectionInactiveIOEvent extends ConnectionIOEvent {

    default boolean isConnectionInactiveIOEvent() {
        return true;
    }

    static ConnectionInactiveIOEvent create(final Connection connection, final long arrivalTime) {
        return new ConnectionInactiveIOEventImpl(connection, arrivalTime);
    }

    class ConnectionInactiveIOEventImpl extends IOEventImpl implements ConnectionInactiveIOEvent {
        private ConnectionInactiveIOEventImpl(final Connection connection, final long arrivalTime) {
            super(connection, arrivalTime);
        }
    }

}
