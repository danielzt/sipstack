package io.sipstack.transport.impl;

import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.event.ConnectionClosedIOEvent;
import io.sipstack.netty.codec.sip.event.ConnectionInactiveIOEvent;
import io.sipstack.netty.codec.sip.event.ConnectionOpenedIOEvent;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.transaction.impl.MockChannel;
import io.sipstack.transport.event.FlowTerminatedEvent;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertThat;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultTransportLayerTest extends TransportLayerTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * There are several ways for a flow to get created. For UDP, we typically create a new
     * flow when we receive a new incoming SIP message. For TCP, we typically get the {@link ConnectionOpenedIOEvent}
     * event.
     *
     * @throws Exception
     */
    @Test
    public void testCreateFlowOnConnectionOpenedIOEvent() throws Exception {
        initiateNewFlow();
    }

    /**
     * For UDP based traffic, a flow will typically be created based off of the very first
     * incoming SIP Message.
     *
     * @throws Exception
     */
    @Test
    public void testCreateFlowOnIncomingSipMessage() throws Exception {
        final InetSocketAddress remoteAddress = new InetSocketAddress("192.168.0.100", 9999);
        final MockChannel channel = createNewChannel(remoteAddress);
        final Connection connection = initiateNewFlow(channel, defaultInviteRequest);
    }

    @Test
    public void testSipOptionsPingForTcpFlow() throws Exception {
        final InetSocketAddress remoteAddress = new InetSocketAddress("192.168.0.100", 9999);
        final MockChannel channel = createNewChannel(remoteAddress);
        final Connection connection = initiateNewFlow(channel, defaultInviteRequest);

    }


    /**
     * When someone connects over TCP we will create a channel but it is possible for an attacker
     * to just establish a bunch of connections and not actually do anything with them. Therefore
     * we must be able to kill the connection if we don't see it being used for a short amount of
     * time. This tests that.
     *
     * @throws Exception
     */
    @Test
    public void testFlowShutsDownDueToNoInitialTraffic() throws Exception {
        final MockChannel channel = initiateNewFlow();
        assertThat(channel.hasCloseBeenCalled(), CoreMatchers.is(false));
        defaultScheduler.fire(SipTimer.Timeout1);
        assertThat(channel.hasCloseBeenCalled(), CoreMatchers.is(true));
    }

    /**
     * Ensure that if someone just establish a TCP connection and then shuts it down
     * again that we handle it gracefully. This is another form of attack that must be handled.
     *
     * @throws Exception
     */
    @Test
    public void testFlowConnectDisconnect() throws Exception {
        final InetSocketAddress remoteAddress = new InetSocketAddress("192.168.0.100", 9999);
        final MockChannel channel = createNewChannel(remoteAddress);
        final Connection connection = initiateNewFlow(channel);
        assertThat(channel.hasCloseBeenCalled(), CoreMatchers.is(false));

        // now, fire an inactive event, which should move us over to the closing
        // state
        IOEvent event = ConnectionInactiveIOEvent.create(connection, defaultClock.getCurrentTimeMillis());
        transportLayer.userEventTriggered(defaultChannelCtx, event);
        assertThat(channel.hasCloseBeenCalled(), CoreMatchers.is(true));

        // and once the channel is closed, Netty will issue yet another event
        // stating that it is closed, which we are converting into a closed event
        // so create and send that one through as well.
        event = ConnectionClosedIOEvent.create(connection, defaultClock.getCurrentTimeMillis());
        transportLayer.userEventTriggered(defaultChannelCtx, event);

        // so now there should have been a life-cycle event saying that
        // the flow is dead.
        final FlowTerminatedEvent terminated = defaultChannelCtx.findForwardedMessageByType(FlowTerminatedEvent.class);
        assertThat(terminated.flow().id(), CoreMatchers.is(connection.id()));

        // and it should not be in the storage anymore
        assertFlowDoesNotExist(connection);
    }


}