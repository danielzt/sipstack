package io.sipstack.transport.impl;

import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.transport.Flow;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public abstract class InternalFlow implements Flow {

    private final Optional<Connection> connection;

    protected InternalFlow(final Optional<Connection> connection) {
        this.connection = connection;
    }

    public Optional<Connection> connection() {
        return connection;
    }

    @Override
    public Optional<ConnectionId> id() {
        if (connection.isPresent()) {
            return Optional.of(connection.get().id());
        }
        return Optional.empty();
    }


}
