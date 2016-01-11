/**
 * 
 */
package io.sipstack.netty.codec.sip;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import io.pkts.packet.sip.Transport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jonas@jonasborjesson.com
 * 
 */
public class ConnectionIdTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testLocalConnectionEndpointId() throws Exception {
        final ConnectionId id1 = createConnection(Transport.udp, "192.168.0.101", 7777, "10.36.10.11", 8765);
        final ConnectionEndpointId remoteId1 = id1.getLocalConnectionEndpointId();

        assertThat(remoteId1.getIpAddress(), is("192.168.0.101"));
        assertThat(remoteId1.getRawIpAddress(), is(id1.getRawLocalIpAddress()));
        assertThat(remoteId1.getPort(), is(7777));

        assertThat(remoteId1.toString(), is("udp:192.168.0.101:7777"));
    }

    @Test
    public void testRemoteConnectionEndpointId() throws Exception {
        final ConnectionId id1 = createConnection(Transport.udp, "192.168.0.101", 7777, "10.36.10.11", 8765);
        final ConnectionEndpointId remoteId1 = id1.getRemoteConnectionEndpointId();

        assertThat(remoteId1.getIpAddress(), is("10.36.10.11"));
        assertThat(remoteId1.getRawIpAddress(), is(id1.getRawRemoteIpAddress()));
        assertThat(remoteId1.getPort(), is(8765));

        assertThat(remoteId1.toString(), is("udp:10.36.10.11:8765"));
    }

    @Test
    public void testRemoteConnectionEndpointIdHashCode() throws Exception {
        final Map<ConnectionEndpointId, String> ids = new HashMap<>();
        final ConnectionEndpointId id1 = createConnection(Transport.udp, "192.168.0.101", 7777, "10.36.10.11", 8765).getRemoteConnectionEndpointId();
        final ConnectionEndpointId id2 = createConnection(Transport.tls, "192.168.0.102", 8888, "10.36.10.12", 8766).getRemoteConnectionEndpointId();
        final ConnectionEndpointId id3 = createConnection(Transport.tls, "192.168.0.103", 9999, "10.36.10.13", 8767).getRemoteConnectionEndpointId();

        ids.put(id1, "one");
        ids.put(id2, "two");
        ids.put(id3, "three");

        assertThat(ids.size(), is(3));
        assertThat(ids.get(id1), is("one"));
        assertThat(ids.get(id2), is("two"));
        assertThat(ids.get(id3), is("three"));
    }

    @Test
    public void testConnetionIdForHashCode() {
        final Map<ConnectionId, String> ids = new HashMap<ConnectionId, String>();
        final ConnectionId id1 = createConnection(Transport.udp, "192.168.0.101", 7777, "10.36.10.11", 8765);
        final ConnectionId id2 = createConnection(Transport.tls, "192.168.0.102", 8888, "10.36.10.12", 8766);
        final ConnectionId id3 = createConnection(Transport.tls, "192.168.0.103", 9999, "10.36.10.13", 8767);

        ids.put(id1, "one");
        ids.put(id2, "two");
        ids.put(id3, "three");

        assertThat(ids.size(), is(3));
        assertThat(ids.get(id1), is("one"));
        assertThat(ids.get(id2), is("two"));
        assertThat(ids.get(id3), is("three"));

        final ConnectionId id1Clone = encodeDecode(id1);
        assertThat(ids.get(id1Clone), is("one"));
        final ConnectionId id1New = createConnection(Transport.udp, "192.168.0.101", 7777, "10.36.10.11", 8765);

        ids.put(id1New, "new one");
        assertThat(ids.get(id1), is("new one"));

        // make sure that protocol actually matters
        final ConnectionId id1tcp = createConnection(Transport.tcp, "192.168.0.101", 7777, "10.36.10.11", 8765);
        assertThat(ids.containsKey(id1tcp), is(false));
        ids.put(id1tcp, "one tcp");
        assertThat(ids.size(), is(4));
        assertThat(ids.get(id2), is("two"));
        assertThat(ids.get(id3), is("three"));
    }

    /**
     * Make sure that the equals method is working as expected
     */
    @Test
    public void testConnectionIdForEquals() {
        final ConnectionId id1 = createConnection(Transport.udp, "192.168.0.101", 7777, "10.36.10.11", 8765);
        final ConnectionId id2 = createConnection(Transport.tls, "192.168.0.102", 8888, "10.36.10.12", 8766);
        final ConnectionId id3 = createConnection(Transport.tls, "192.168.0.103", 9999, "10.36.10.13", 8767);

        assertConnectionIdEncodeDecodeCorrectly(id1);
        assertConnectionIdEncodeDecodeCorrectly(id2);
        assertConnectionIdEncodeDecodeCorrectly(id3);

        assertThat(id1, is(id1));
        assertThat(id1, not(id2));
        assertThat(id2, not(id3));
        assertThat(id3, not(id2));

        // and also make sure that the protocol matters
        final ConnectionId id2udp = createConnection(Transport.udp, "192.168.0.102", 8888, "10.36.10.12", 8766);
        assertThat(id2udp, not(id1));
        assertThat(id2udp, not(id2));
        assertThat(id2udp, not(id3));
    }

    /**
     * Helper method for creating a {@link NetworkConnection}.
     * 
     * @param protocol
     * @param localIp
     * @param localPort
     * @param remoteIp
     * @param remotePort
     * @return
     */
    private ConnectionId createConnection(final Transport protocol, final String localIp, final int localPort,
            final String remoteIp, final int remotePort) {
        final InetSocketAddress localAddress = new InetSocketAddress(localIp, localPort);
        final InetSocketAddress remoteAddress = new InetSocketAddress(remoteIp, remotePort);
        final Channel channel = mock(Channel.class);
        when(channel.localAddress()).thenReturn(localAddress);
        Connection connection  = null;
        if (protocol == Transport.udp) {
            connection = new UdpConnection(channel, remoteAddress);
        } else if (protocol == Transport.tcp || protocol == Transport.tls) {
            connection = new TcpConnection(channel, remoteAddress);
        }

        assertThat(connection.getLocalIpAddress(), is(localIp));
        assertThat(connection.getLocalPort(), is(localPort));

        assertThat(connection.getRemoteIpAddress(), is(remoteIp));
        assertThat(connection.getRemotePort(), is(remotePort));
        return connection.id();
    }

    /**
     * Make sure that the encode/decode produces the expected result
     * 
     * @param id
     */
    private void assertConnectionIdEncodeDecodeCorrectly(final ConnectionId id) {
        final ConnectionId id2 = encodeDecode(id);
        assertThat(id, is(id2));
        assertThat(id.hashCode(), is(id2.hashCode()));
    }

    /**
     * Helper method to encode and then decode the id back again.
     * 
     * @param id
     * @return
     */
    private ConnectionId encodeDecode(final ConnectionId id) {
        final String encoded = id.encode().toString();
        return ConnectionId.decode(encoded);
    }

}
