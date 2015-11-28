package io.sipstack.transport.impl;

import io.sipstack.config.FlowConfiguration;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.transport.FlowId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultFlowStorage implements FlowStorage {

    private final TransportLayerConfiguration config;

    private final Map<FlowId, FlowActor> flows;

    public DefaultFlowStorage(final TransportLayerConfiguration config) {
        this.config = config;
        this.flows = new ConcurrentHashMap<>(config.getFlow().getDefaultStorageSize());
    }

    public FlowActor ensureFlow(final Connection connection) {
        final FlowId flowId = FlowId.create(connection.id());
        return flows.computeIfAbsent(flowId, obj -> new DefaultFlowActor(config, flowId, connection));
    }

    public FlowActor get(final FlowId id) {
        return flows.get(id);
    }

    @Override
    public void remove(final FlowId id) {
        flows.remove(id);
    }

}
