package io.sipstack.transport.impl;

import com.sun.javafx.UnmodifiableArrayList;
import io.sipstack.config.FlowConfiguration;
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
public class DefaultFlowStorage implements FlowStorage {

    private final TransportLayerConfiguration config;

    private final Clock clock;

    private final Map<ConnectionEndpointId, FlowBucket> buckets;

    private final int lockCount = Runtime.getRuntime().availableProcessors() * 2;
    private final Object[] locks = new Object[lockCount];

    public DefaultFlowStorage(final TransportLayerConfiguration config, final Clock clock) {
        this.config = config;
        this.buckets = new HashMap<>(config.getFlow().getDefaultStorageSize());
        this.clock = clock;
        for (int i = 0; i < locks.length; ++i) {
            locks[i] = new Object();
        }
    }

    @Override
    public List<FlowActor> getFlows(final ConnectionEndpointId remoteEndpoint) {
        synchronized(getLock(remoteEndpoint)) {
            final FlowBucket bucket = buckets.get(remoteEndpoint);
            if (bucket != null) {
                return bucket.getFlows();
            }
            return Collections.emptyList();
        }
    }

    public FlowActor ensureFlow(final Connection connection) {
        final ConnectionId connectionId = connection.id();
        final ConnectionEndpointId endpointId = connectionId.getRemoteConnectionEndpointId();
        synchronized(getLock(endpointId)) {
            FlowBucket bucket = buckets.get(endpointId);
            FlowActor flow = null;
            if (bucket == null) {
                bucket = new FlowBucket(config, endpointId);
                buckets.put(endpointId, bucket);
            } else {
                flow = bucket.getFlow(connectionId);
            }

            if (flow == null) {
                final FlowId flowId = FlowId.create(connectionId);
                flow = new DefaultFlowActor(config, flowId, connection, clock);
                bucket.store(connectionId, flow);
            }

            return flow;
        }

    }

    private ConnectionEndpointId createEndpointId(final Connection connection) {
        final ConnectionId id = connection.id();
        return ConnectionEndpointId.create(id.getProtocol(), id.getRemoteAddress());
    }

    private Object getLock(final ConnectionEndpointId id) {
        final int lockId = Math.abs(id.hashCode() % lockCount);
        return locks[lockId];
    }

    @Override
    public FlowActor get(final ConnectionEndpointId id) {
        synchronized(getLock(id)) {
            final FlowBucket bucket = buckets.get(id);
            if (bucket == null) {
                return null;
            }

            return bucket.pickAnyFlow();
        }
    }

    @Override
    public FlowActor get(final ConnectionId id) {
        final ConnectionEndpointId endpointId = id.getRemoteConnectionEndpointId();
        synchronized(getLock(endpointId)) {
            final FlowBucket bucket = buckets.get(endpointId);
            if (bucket != null) {
                return bucket.getFlow(id);
            }
            return null;
        }
    }

    public FlowActor get(final FlowId id) {
        throw new RuntimeException("TODO");
    }

    @Override
    public void remove(final ConnectionId id) {
        final ConnectionEndpointId endpointId = id.getRemoteConnectionEndpointId();
        synchronized(getLock(endpointId)) {
            final FlowBucket bucket = buckets.get(endpointId);
            if (bucket != null) {
                bucket.removeFlow(id);
            }
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

        private final List<FlowActor> flows;

        private final Random random = new Random(System.currentTimeMillis());

        private FlowBucket(final TransportLayerConfiguration config, final ConnectionEndpointId id ) {
            this.id = id;
            // TODO: should probably be another storage size...
            // flows = new HashMap<>(config.getFlow().getDefaultStorageSize());
            flows = new ArrayList<>(10);
        }

        protected int count() {
            return flows.size();
        }

        public List<FlowActor> getFlows() {
            return Collections.unmodifiableList(flows);
        }

        public FlowActor getFlow(final ConnectionId id) {
            return flows.stream().filter(flow -> flow.connection().id().equals(id)).findFirst().orElse(null);
        }

        public void removeFlow(final ConnectionId id) {
            for (int i = 0; i < flows.size(); ++i) {
                if (flows.get(i).flow().id().equals(id)) {
                    flows.remove(i);
                }
            }
        }

        public void store(final ConnectionId id, final FlowActor flow) {
            flows.add(flow);
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

            if (size == 1) {
                return flows.get(0);
            }

            // TODO: for now, let's pick one at random but we may
            // want to have a policy where we pick the one that
            // is e.g. the least busy based on some criteria such
            // as bandwidth, any congestion failures?, messages / second
            // etc
            return flows.get(random.nextInt(size));
        }
    }

}
