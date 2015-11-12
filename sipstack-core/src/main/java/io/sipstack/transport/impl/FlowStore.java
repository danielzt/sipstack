package io.sipstack.transport.impl;

import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.transport.FlowId;

/**
 * @author jonas@jonasborjesson.com
 */
public interface FlowStore {

    /**
     * Ensure a flow exists for this connection object. If one didn't exist, a new
     * {@link FlowActor} will be created.
     *
     * @param connection
     * @return
     */
    FlowActor ensureFlow(Connection connection);

    FlowActor get(FlowId id);
}
