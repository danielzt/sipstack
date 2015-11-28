package io.sipstack.transport.impl;

import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowState;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public abstract class InternalFlow implements Flow {

    private final Optional<Connection> connection;

    private final FlowState state;

    protected InternalFlow(final Optional<Connection> connection, final FlowState state) {
        this.connection = connection;
        this.state = state;
    }

    public Optional<Connection> connection() {
        return connection;
    }

    public FlowState getState() {
        return state;
    }

    @Override
    public Optional<ConnectionId> id() {
        if (connection.isPresent()) {
            return Optional.of(connection.get().id());
        }
        return Optional.empty();
    }


}
