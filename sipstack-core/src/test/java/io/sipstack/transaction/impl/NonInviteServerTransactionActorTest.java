package io.sipstack.transaction.impl;

import io.sipstack.SipStackTestBase;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.netty.codec.sip.DefaultSipMessageEvent;
import io.sipstack.netty.codec.sip.SipMessageEvent;
import io.sipstack.netty.codec.sip.Transport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jonas@jonasborjesson.com
 */
public class NonInviteServerTransactionActorTest extends SipStackTestBase {

    private Connection defaultConnection;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        final ConnectionId id = createConnectionId(Transport.udp, "10.36.10.100", 5060, "192.168.0.100", 5090);
        defaultConnection = mockConnection(id);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private ConnectionId createConnectionId(final Transport protocol, final String localIp, final int localPort,
                                          final String remoteIp, final int remotePort) {
        final InetSocketAddress localAddress = new InetSocketAddress(localIp, localPort);
        final InetSocketAddress remoteAddress = new InetSocketAddress(remoteIp, remotePort);
        return ConnectionId.create(protocol, localAddress, remoteAddress);
    }

    private Connection mockConnection(final ConnectionId id) {
        final Connection connection = mock(Connection.class);
        when(connection.id()).thenReturn(id);
        when(connection.getTransport()).thenReturn(id.getProtocol());
        return connection;
    }


    /**
     * @throws Exception
     */
    @Test
    public void testBye() throws Exception {
        final SipMessageEvent bye = new DefaultSipMessageEvent(defaultConnection, defaultByeRequest, 0);
        actor.tellAnonymously(bye);
        Thread.sleep(40000);
    }
}