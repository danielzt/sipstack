package io.sipstack.transport.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.Transport;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.ControllableClock;
import io.sipstack.MockChannelHandlerContext;
import io.sipstack.MockScheduler;
import io.sipstack.SipStackTestBase;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.netty.codec.sip.*;
import io.sipstack.netty.codec.sip.event.ConnectionOpenedIOEvent;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.transaction.impl.DefaultTransactionLayer;
import io.sipstack.transaction.impl.MockChannel;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowId;
import io.sipstack.transport.FlowState;
import io.sipstack.transport.event.FlowEvent;
import org.hamcrest.CoreMatchers;
import org.junit.Before;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static io.pkts.packet.sip.Transport.tcp;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jonas@jonasborjesson.com
 */
public class TransportLayerTestBase extends SipStackTestBase {

    protected ControllableClock defaultClock;

    protected DefaultTransportLayer transportLayer;

    protected FlowStorage defaultFlowStorage;

    /**
     * The IP Address to use for the connection, which represents
     * our local ip-address.
     */
    protected String defaultLocalIPAddress = "192.168.0.100";
    protected int defaultLocalPort = 6789;

    /**
     * The IP address, or host, to use for the connection. It
     * represents the remote ip-address, i.e. to where we
     * have a flow (for tcp also equal to where we are connected)
     */
    protected String defaultRemoteIPAddress = "62.63.64.65";
    protected int defaultRemotePort = 7080;

    protected SipURI defaultVipAddress = null;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        defaultClock = new ControllableClock();

        final TransportLayerConfiguration config = new TransportLayerConfiguration();
        config.getFlow().setDefaultStorageSize(100);
        reset(config);
    }

    public void reset(final TransportLayerConfiguration config) {
        defaultScheduler = new MockScheduler(new CountDownLatch(1));
        defaultFlowStorage = new DefaultFlowStorage(config, defaultClock);
        transportLayer = createTransportLayer(config);
        defaultChannelCtx = new MockChannelHandlerContext(transportLayer);
    }

    public DefaultTransportLayer createTransportLayer(final TransportLayerConfiguration config) {
        return new DefaultTransportLayer(config, defaultClock, defaultFlowStorage, defaultScheduler);
    }

    /**
     * Convenience method for making sure that a flow actually exists in the flow storage.
     * @param id
     */
    public void assertFlowExists(final FlowId id) {
        assertThat(defaultFlowStorage.get(id), not((FlowId)null));
    }

    public void assertFlowExists(final Connection connection) {
        assertFlowExists(FlowId.create(connection.id()));
    }

    public void assertFlowDoesNotExist(final FlowId id) {
        assertThat(defaultFlowStorage.get(id), is((FlowId)null));
    }

    public void assertFlowDoesNotExist(final Connection connection) {
        assertFlowDoesNotExist(FlowId.create(connection.id()));
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
    public Connection initiateNewFlow(final MockChannel channel, final SipMessage msg) throws Exception {
        return initiateNewFlow(tcp, channel, msg, null);
    }

    public Connection initiateNewFlow(final Transport transport, final MockChannel channel, final SipMessage msg, final SipURI vipAddress) throws Exception {
        final Connection connection = createConnection(transport, channel, vipAddress);
        final IOEvent event = msg != null ?
                IOEvent.create(connection, msg) :
                ConnectionOpenedIOEvent.create(connection, defaultClock.getCurrentTimeMillis());

        if (msg != null) {
            transportLayer.channelRead(defaultChannelCtx, event);
        } else {
            // the timer Timeout1 is used as the initial idle timeout value
            // and is only scheduled when we move into the READY state, which
            // will only do when we get a ConnectionOpenedIOEvent
            transportLayer.userEventTriggered(defaultChannelCtx, event);
            assertTimerScheduled(SipTimer.Timeout1);
        }

        assertFlowExists(connection);

        if (msg != null) {
            // The sip message that created the flow should have been forwarded up
            // the chain so make sure that it actually did.
            final FlowEvent forwarded = defaultChannelCtx.findForwardedMessageByType(FlowEvent.class);
            assertThat(forwarded.isSipFlowEvent(), is(true));
            assertThat(forwarded.toSipFlowEvent().message(), is(msg));
        }

        return connection;
    }

    public Connection createConnection(final Transport transport, final MockChannel channel, final SipURI vipAddress) {
        switch (transport) {
            case udp:
                return new UdpConnection(channel, (InetSocketAddress)channel.remoteAddress(), Optional.ofNullable(vipAddress));
            case tcp:
                return new TcpConnection(channel, (InetSocketAddress)channel.remoteAddress(), Optional.ofNullable(vipAddress));
            case tls:
            case sctp:
            case ws:
            case wss:
            default:
                throw new IllegalArgumentException("Dont have connections for that transport just yet");
        }
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
    public Connection initiateNewFlow(final MockChannel channel) throws Exception {
        return initiateNewFlow(channel, null);
    }

    /**
     * Convenience method for initiating a new flow to the ACTIVE state.
     *
     * @param config
     * @param transport
     * @return an array where the first element is a Connection object, the
     *         second is the Flow and the third is the MockChannel.
     * @throws Exception
     */
    public Object[] initiateFlowToActive(final TransportLayerConfiguration config, final Transport transport) throws Exception {
        reset(config);

        final InetSocketAddress remoteAddress = new InetSocketAddress(defaultRemoteIPAddress, defaultRemotePort);
        final MockChannel channel = createNewChannel(remoteAddress);
        final Connection connection = initiateNewFlow(transport, channel, defaultInviteRequest, defaultVipAddress);
        final Flow flow = defaultChannelCtx.findForwardedMessageByType(FlowEvent.class).flow();
        assertThat(flow.getState(), is(FlowState.ACTIVE));
        return new Object[]{connection, flow, channel};
    }

    public MockChannel initiateNewFlow() throws Exception {
        final InetSocketAddress remoteAddress = new InetSocketAddress(defaultRemoteIPAddress, defaultRemotePort);
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
        final InetSocketAddress localAddress = new InetSocketAddress(defaultLocalIPAddress, defaultLocalPort);

        // TODO: want to mock up the channel so that it
        // returns the correct values as well.

        final ChannelHandlerContext ctx = defaultChannelCtx;
        final ChannelOutboundHandler handler = transportLayer;

        return new MockChannel(ctx, handler, localAddress, remoteAddress);
    }


}
