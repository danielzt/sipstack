package io.sipstack.transport.impl;

import io.pkts.packet.sip.Transport;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.netty.codec.sip.*;
import io.sipstack.transaction.impl.MockChannel;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultFlowStorageTest extends TransportLayerTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();;
    }

    /**
     * Just ensure that we get two different flows when we create one UDP and one TCP
     * to and from the same ip:port pair
     * @throws Exception
     */
    @Test
    public void testTcpAndUdpEndpointsAreIndeedDifferent() throws Exception {
        final InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 5060);
        final InetSocketAddress remoteAddress = new InetSocketAddress("192.168.0.100", 5060);
        final FlowActor udpFlow = defaultFlowStorage.ensureFlow(createUdpConnection(localAddress, remoteAddress));
        final FlowActor tcpFlow = defaultFlowStorage.ensureFlow(createTcpConnection(localAddress, remoteAddress));
        assertThat(udpFlow, not(tcpFlow));
    }

    /**
     * Quite often you may just want to get a flow that is connected to a remote address but
     * if we have many connections we don't necessarily care which connection we use.
     *
     * @throws Exception
     */
    @Test
    public void testGetAnyFlow() throws Exception {
        // create a few that points to the same remote ip...
        final String ip = "12.13.14.15";
        final int port = 5080;
        populateStorageBasedOnRemoteAddress(ip, port, 10);

        // ask to get any flow pointing to that remote address
        final ConnectionEndpointId remoteEndpointId = ConnectionEndpointId.create(Transport.udp, new InetSocketAddress(ip, port));
        final FlowActor actor = defaultFlowStorage.get(remoteEndpointId);
        assertThat(actor, not((FlowActor)null));
    }


    @Test
    public void testEnsureFlowAlwaysSameBucket() throws Exception {
        final InetSocketAddress remoteAddress = new InetSocketAddress("192.168.0.100", 5060);
        for (int i = 0; i < 100; ++i) {
            final InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 5060 + i);
            final FlowActor actor = defaultFlowStorage.ensureFlow(createUdpConnection(localAddress, remoteAddress));
            assertThat(actor, not((FlowActor) null));
            assertThat(defaultFlowStorage.count(), is(i + 1));
        }

        // so all of them should be sorted under the same connection endpoint
        final ConnectionEndpointId endpointId = ConnectionEndpointId.create(Transport.udp, remoteAddress);
        assertThat(defaultFlowStorage.getFlows(endpointId).size(), is(100));

        // we should be able to get them...
        for (int i = 0; i < 100; ++i) {
            final InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 5060 + i);
            final FlowActor actor = defaultFlowStorage.get(createUdpConnection(localAddress, remoteAddress).id());
            assertThat(actor, not((FlowActor) null));
        }

        // and remove them (only half)
        for (int i = 0; i < 100; i += 2) {
            final InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 5060 + i);
            defaultFlowStorage.remove(createUdpConnection(localAddress, remoteAddress).id());
        }
        assertThat(defaultFlowStorage.count(), is(50));

        // still, since all of them are pointing to the same remote address and transport
        // they should still be accessible via the getFlows...
        assertThat(defaultFlowStorage.getFlows(endpointId).size(), is(50));
    }

    /**
     * Populate the storage but keep the remote address the same while the local address keeps changing.
     *
     * @param remoteIp
     * @param remotePort
     * @param count
     */
    private void populateStorageBasedOnRemoteAddress(final String remoteIp, final int remotePort, final int count) {
        final InetSocketAddress remoteAddress = new InetSocketAddress(remoteIp, remotePort);
        for (int i = 0; i < count; ++i) {
            final InetSocketAddress localAddress = new InetSocketAddress("62.63.64.65", 5060 + i);
            final Connection connection = createUdpConnection(localAddress, remoteAddress);
            final FlowActor actor = defaultFlowStorage.ensureFlow(connection);
            assertThat(actor, not((FlowActor) null));
        }
    }

    private void populateStorageBasedOnLocalAddress(final String localIp, final int localPort, final int count) {
        final InetSocketAddress localAddress = new InetSocketAddress(localIp, localPort);
        for (int i = 0; i < count; ++i) {
            final InetSocketAddress remoteAddress = new InetSocketAddress("62.63.64.65", 5060 + i);
            final Connection connection = createUdpConnection(localAddress, remoteAddress);
            final FlowActor actor = defaultFlowStorage.ensureFlow(connection);
            assertThat(actor, not((FlowActor) null));
        }
    }

}