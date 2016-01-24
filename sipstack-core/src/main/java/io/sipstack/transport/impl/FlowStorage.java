package io.sipstack.transport.impl;

import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionEndpointId;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.transport.FlowId;

import java.util.List;

/**
 * A {@link Flow} is stored under two keys. The main key is the
 * {@link ConnectionEndpointId}, which points to the remote endpoint
 * and typically is what you want to use for lookup as you establish
 * new connections. The {@link Flow} is then also stored under the
 * {@link ConnectionId} since the flow does represent the flow between
 * a local IP:Port and a remote IP:port and the transport used between them.
 *
 * @author jonas@jonasborjesson.com
 */
public interface FlowStorage {

    /**
     * Ensure a flow exists for this connection object. If one didn't exist, a new
     * {@link FlowActor} will be created.
     *
     * @param connection
     * @return
     */
    FlowActor ensureFlow(Connection connection);

    /**
     * Get any {@link FlowActor} that "points" to the remote endpoint.
     * That means that if there are multiple flows that are all connected
     * to that endpoint, one will be picked based on local policy (e.g.
     * the flow that is the least busy). Usually, if you try to establish
     * a new connection to e.g. 192.168.0.100:5060 and you are using
     * TCP then there may be multiple TCP connections from our host
     * to this remote endpoint. However, the local IP:port will vary
     * since with TCP, you will get a different local IP:port everytime.
     * However, you may not care because this is the first time you
     * are sending a SIP message to that remote host so you can
     * pick any flow connected to that host. If that is the case, use
     * this method.
     *
     * @param id
     * @return
     */
    FlowActor get(ConnectionEndpointId id);

    FlowActor get(ConnectionId id);

    /**
     * Get all flows pointing to the same remote endpoint. I.e. all the flows that
     * is "connected" to the same remote IP:port and is using the same transport.
     *
     * @param remoteEndpoint
     * @return a list of flows pointing to the same remote IP:port + transport
     * or an empty list of there are none.
     */
    List<FlowActor> getFlows(ConnectionEndpointId remoteEndpoint);

    void remove(ConnectionId id);

    int count();
}
