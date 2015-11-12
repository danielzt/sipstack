package io.sipstack.transport.impl;

import io.sipstack.config.FlowConfiguration;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.transport.FlowId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultFlowStore implements FlowStore {

    private final FlowConfiguration config;

    private final Map<FlowId, FlowActor> flows;

    public DefaultFlowStore(final FlowConfiguration config) {
        this.config = config;
        this.flows = new ConcurrentHashMap<>(config.getDefaultStorageSize());
    }

    public FlowActor ensureFlow(final Connection connection) {
        final FlowId flowId = FlowId.create(connection.id());
        final FlowActor actor = flows.computeIfAbsent(flowId, obj -> {
            System.err.println("Got a new flow going");
            return new DefaultFlowActor(config, connection);
        });

        return actor;
    }

    public FlowActor get(final FlowId id) {
        return flows.get(id);
    }

}
