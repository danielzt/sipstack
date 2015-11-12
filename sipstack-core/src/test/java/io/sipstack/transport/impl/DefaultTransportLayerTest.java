package io.sipstack.transport.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.netty.codec.sip.*;
import io.sipstack.netty.codec.sip.event.ConnectionActiveIOEvent;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.transaction.impl.MockChannel;
import io.sipstack.transaction.impl.MockChannelHandlerContext;
import io.sipstack.transport.TransportLayer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.*;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultTransportLayerTest extends TransportLayerTestBase {

    protected DefaultTransportLayer transportLayer;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        final TransportLayerConfiguration config = new TransportLayerConfiguration();
        config.getFlow().setDefaultStorageSize(100);

        transportLayer = new DefaultTransportLayer(config, defaultClock, null);
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testCreateFlow() throws Exception {
        final Connection connection = createTcpConnection();
        final IOEvent event = ConnectionActiveIOEvent.create(connection, defaultClock.getCurrentTimeMillis());
        transportLayer.userEventTriggered(defaultChannelCtx, event);
    }

    public Connection createTcpConnection() {
        final InetSocketAddress remoteAddress = new InetSocketAddress("192.168.0.100", 9999);
        final InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 5060);

        // TODO: want to mock up the channel so that it
        // returns the correct values as well.

        // TODO: what should be choose here...
        final ChannelHandlerContext ctx = null;
        final ChannelOutboundHandler handler = null;

        final Channel channel = new MockChannel(ctx, handler, localAddress, remoteAddress);
        return new TcpConnection(channel, remoteAddress);
    }



}