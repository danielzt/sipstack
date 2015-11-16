package io.sipstack.transport.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.*;
import io.sipstack.netty.codec.sip.event.*;
import io.sipstack.transaction.impl.MockChannel;
import io.sipstack.transport.FlowId;
import io.sipstack.transport.event.FlowTerminatedEvent;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.*;

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
        defaultScheduler.fire(SipTimer.Timeout);
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
        assertThat(terminated.flow().id().get(), CoreMatchers.is(connection.id()));

        // and it should not be in the storage anymore
        assertFlowDoesNotExist(connection);
    }

    /**
     * Convenience method for initiating a new flow based off an event, which is either a {@link SipMessage} or
     * if you pass in null (slightly awkward) then a {@link ConnectionOpenedIOEvent} will be passed in instead.
     *
     * Also note that SIP messages will be passed through the netty pipeline through the
     * channelRead method but the various connection events is pushed through
     * the userEventTriggered.
     *
     * Also note that a timer is scheduled for both events but with different values.
     * For the {@link ConnectionOpenedIOEvent} the timer is for ensuring that we get
     * something across the flow and if not, the flow will be killed off.
     * For {@link SipMessage}s, which takes us to the ACTIVE state right away, then
     * the timout is for keep-alive traffic.
     *
     * @param channel
     * @param msg
     * @return
     * @throws Exception
     */
    private Connection initiateNewFlow(final MockChannel channel, final SipMessage msg) throws Exception {
        final Connection connection = createTcpConnection(channel, (InetSocketAddress)channel.remoteAddress());
        final IOEvent event = msg != null ?
                IOEvent.create(connection, msg) :
                ConnectionOpenedIOEvent.create(connection, defaultClock.getCurrentTimeMillis());

        if (msg != null) {
            transportLayer.channelRead(defaultChannelCtx, event);
            assertTimerScheduled(SipTimer.Timeout); // TODO: need to check the duration scheduled
        } else {
            transportLayer.userEventTriggered(defaultChannelCtx, event);
            assertTimerScheduled(SipTimer.Timeout); // TODO: need to check the duration scheduled
        }

        assertFlowExists(connection);
        return connection;
    }

    /**
     * Initiate a new flow based on a {@link ConnectionOpenedIOEvent}. If you rather initiate
     * a new flow based off of a {@link SipMessage} then just
     * use the {@link DefaultTransportLayerTest#initiateNewFlow(MockChannel, SipMessage)} instead.
     *
     * @param channel
     * @return
     * @throws Exception
     */
    private Connection initiateNewFlow(final MockChannel channel) throws Exception {
        return initiateNewFlow(channel, null);
    }

    private MockChannel initiateNewFlow() throws Exception {
        final InetSocketAddress remoteAddress = new InetSocketAddress("192.168.0.100", 9999);
        final MockChannel channel = createNewChannel(remoteAddress);
        initiateNewFlow(channel);
        return channel;
    }

    public Connection createUdpConnection(final Channel channel, final InetSocketAddress remoteAddress) {
        return new UdpConnection(channel, remoteAddress);
    }

    public Connection createTcpConnection(final Channel channel, final InetSocketAddress remoteAddress) {
        return new TcpConnection(channel, remoteAddress);
    }

    public MockChannel createNewChannel(final InetSocketAddress remoteAddress) {
        final InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 5060);

        // TODO: want to mock up the channel so that it
        // returns the correct values as well.

        // TODO: what should be choose here...
        final ChannelHandlerContext ctx = defaultChannelCtx;
        final ChannelOutboundHandler handler = transportLayer;

        return new MockChannel(ctx, handler, localAddress, remoteAddress);
    }



}