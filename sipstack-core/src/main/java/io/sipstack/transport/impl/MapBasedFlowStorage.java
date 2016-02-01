package io.sipstack.transport.impl;

import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionEndpointId;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.transport.FlowId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jonas@jonasborjesson.com
 */
public class MapBasedFlowStorage implements FlowStorage {

    private final TransportLayerConfiguration config;

    private final Clock clock;

    private final ConcurrentHashMap<ConnectionEndpointId, FlowBucket> buckets;

    public MapBasedFlowStorage(final TransportLayerConfiguration config, final Clock clock) {
        this.config = config;
        this.buckets = new ConcurrentHashMap<>(config.getFlow().getDefaultStorageSize());
        this.clock = clock;
    }

    @Override
    public List<FlowActor> getFlows(final ConnectionEndpointId remoteEndpoint) {
        final FlowBucket bucket = buckets.get(remoteEndpoint);
        if (bucket != null) {
            return bucket.getFlows();
        }
        return Collections.emptyList();
    }

    public FlowActor ensureFlow(final Connection connection) {
        final ConnectionId connectionId = connection.id();
        final ConnectionEndpointId endpointId = connectionId.getRemoteConnectionEndpointId();
        final FlowBucket bucket = buckets.computeIfAbsent(endpointId, id -> new FlowBucket(config, id, clock));
        return bucket.ensureFlow(connection);
    }

    private ConnectionEndpointId createEndpointId(final Connection connection) {
        final ConnectionId id = connection.id();
        return ConnectionEndpointId.create(id.getProtocol(), id.getRemoteAddress());
    }

    @Override
    public FlowActor get(final ConnectionEndpointId id) {
        final FlowBucket bucket = buckets.get(id);
        if (bucket == null) {
            return null;
        }

        return bucket.pickAnyFlow();
    }

    @Override
    public FlowActor get(final ConnectionId id) {
        final FlowBucket bucket = buckets.get(id.getRemoteConnectionEndpointId());
        if (bucket != null) {
            return bucket.getFlow(id);
        }
        return null;
    }

    public FlowActor get(final FlowId id) {
        throw new RuntimeException("TODO");
    }

    @Override
    public void remove(final ConnectionId id) {
        final FlowBucket bucket = buckets.get(id.getRemoteConnectionEndpointId());
        if (bucket != null) {
            bucket.removeFlow(id);
        }
    }

    @Override
    public int count() {
        return buckets.values().stream().mapToInt(FlowBucket::count).sum();
    }

    private static class FlowBucket {

        /**
         * All flows within this bucket is "pointing" to the
         * same remote endpoint, which is represented by thie
         * {@link ConnectionEndpointId}
         */
        private final ConnectionEndpointId id;

        private final TransportLayerConfiguration config;

        private final ConcurrentHashMap<ConnectionId, FlowActor> flows;

        private final Clock clock;

        private final Random random = new Random(System.currentTimeMillis());

        private FlowBucket(final TransportLayerConfiguration config, final ConnectionEndpointId id, final Clock clock) {
            this.config = config;
            this.id = id;
            flows = new ConcurrentHashMap<>();
            this.clock = clock;
        }

        protected int count() {
            return flows.size();
        }

        public List<FlowActor> getFlows() {
            return new ArrayList<>(flows.values());
            // return Collections.unmodifiableList(flows.values());
        }

        public FlowActor ensureFlow(final Connection connection) {
            return flows.computeIfAbsent(connection.id(), id -> new DefaultFlowActor(config, FlowId.create(id), connection, clock));
        }

        public FlowActor getFlow(final ConnectionId id) {
            return flows.get(id);
        }

        public void removeFlow(final ConnectionId id) {
            flows.remove(id);
        }

        public void store(final ConnectionId id, final FlowActor flow) {
            flows.put(id, flow);
        }

        /**
         * If the user doesn't care which flow to use, as long as it
         * is "connected" to the remote endpoint (and remember, each
         * bucket is only targeting a particular remote endpoint) then
         * choose one based on some local policy.
         *
         * @return
         */
        public FlowActor pickAnyFlow() {
            final int size = flows.size();
            if (size == 0) {
                return null;
            }

            final Collection<FlowActor> values = flows.values();
            if (size == 1) {
                return values.iterator().next();
            }

            // TODO: for now, let's pick one at random but we may
            // want to have a policy where we pick the one that
            // is e.g. the least busy based on some criteria such
            // as bandwidth, any congestion failures?, messages / second
            // etc

            final List<FlowActor> l = new ArrayList<>(values);
            return l.get(random.nextInt(size));
        }
    }

}
