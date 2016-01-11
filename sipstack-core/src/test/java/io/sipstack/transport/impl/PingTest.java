package io.sipstack.transport.impl;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.Transport;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.config.SipConfiguration;
import io.sipstack.config.SipOptionsPingConfiguration;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.netty.codec.sip.event.SipRequestIOEvent;
import io.sipstack.netty.codec.sip.event.SipResponseIOEvent;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowState;
import io.sipstack.transport.event.FlowEvent;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Contains all tests for testing pings in various variations.
 * This class itself is abstract but its just so that it would
 * be easy to run all tests with a different transport as
 * more and more gets added.
 *
 * @author jonas@jonasborjesson.com
 */
public abstract class PingTest extends TransportLayerTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    public abstract Transport getTransport();


    /**
     * If there is traffic flowing across the flow then we will not issue
     * a PING since the traffic itself is enough to keep the flow open.
     *
     * @throws Exception
     */
    @Test
    public void testNoPingDueToOtherTraffic() throws Exception {
        final Transport transport = getTransport();
        final TransportLayerConfiguration config =
                loadConfiguration(SipConfiguration.class, "PingTest001.yaml").getTransport();
        assertThat("You changed the config file, this test assumes an idle timeout of 40 seconds",
                config.getFlow().getKeepAliveConfiguration().getIdleTimeout().getSeconds(), is(40L));

        final Object[]  objects = initiateFlowToActive(config, transport);
        final Connection connection = (Connection)objects[0];
        final Flow flow = (Flow)objects[1];

        // 20 seconds pass...
        defaultClock.plusSeconds(20);

        // some message is passed through the flow...
        final IOEvent event = IOEvent.create(connection, defaultInviteRequest);
        transportLayer.channelRead(defaultChannelCtx, event);

        // another 10 seconds passes
        defaultClock.plusSeconds(10);

        // the timeout2 timer, which is the ping timer, fires but should now
        // not lead to a options being sent out because the previous message
        // is counted as a ping and that message was received only
        // 10 seconds ago...
        defaultScheduler.fire(SipTimer.Timeout2);
        defaultChannelCtx.assertNothingWritten();

        // the way the scheduling of the idle timer works is that we will
        // reschedule the timer based on the last "ping" or "pong" and since the
        // last message received was the invite request we will use that as
        // the base for when to schedule the next timeout... which will
        // be 30 seconds since the timeout is set for 40 seconds and
        // we received a message 10 seconds ago, which means that if
        // we don't receive another anything within 40 - 10 seconds
        // we should start pinging...
        assertTimerScheduled(SipTimer.Timeout2, Duration.ofSeconds(30));
    }

    /**
     * If we are in the ACTIVE ping mode and have SIP_OPTIONS specified as the
     * preferred mechanism then we should be issuing pings when the idle timer
     * fires. Test that...
     *
     * @throws Exception
     */
    @Test
    public void testSipOptionsPing() throws Exception {
        final Transport transport = getTransport();
        final Object[] objects = initiateFlowToSipOptionsPing(transport);
        final Connection connection = (Connection)objects[0];
        final SipRequest options = (SipRequest)objects[1];

        // Send in the 200 OK to that response which is our pong message.
        // Note, this one should be absorbed by the Flow and not forwarded
        // up the chain...
        final SipResponse response = options.toRequest().createResponse(200).build();
        final SipResponseIOEvent responseEvent = IOEvent.create(connection, response);
        transportLayer.channelRead(defaultChannelCtx, responseEvent);
        defaultChannelCtx.assertSipMessageNotForwarded(response);

        // also, since we are supposed to accept incoming SIP OPTIONS as pings and then
        // we should generate a 200 OK to that ping AND we should NOT forward that option
        // up the stack.
        //
    }

    /**
     * If we issue a SIP options as a ping we are typically waiting for the pong, which should be
     * in the form of a 200 OK. However, if we receive any other type of traffic, let's say
     * an INVITE, then we will use that as a sign that the client on the other side of this
     * flow is alive and healty and as such, we don't really care if we get the 200 OK to the
     * options or not.
     *
     * @param transport
     * @throws Exception
     */
    @Test
    public void testSipOptionsPingPongFromOtherTraffic() throws Exception {
        final Transport transport = getTransport();
        final Object[] objects = initiateFlowToSipOptionsPing(transport);
        final Connection connection = (Connection)objects[0];
        final SipRequest options = (SipRequest)objects[1];

        // Send in an INVITE, which should transition us over to the ACTIVE state again
        // and the invite itself should be forwarded up the stack.
        final SipRequestIOEvent requestEvent = IOEvent.create(connection, defaultInviteRequest);
        transportLayer.channelRead(defaultChannelCtx, requestEvent);
        defaultChannelCtx.assertSipMessageForwarded(defaultInviteRequest);
        final Flow flow = defaultChannelCtx.findForwardedMessageByType(FlowEvent.class).flow();
        assertThat(flow.getState(), is(FlowState.ACTIVE));

        // However, if we do get the 200 OK in we should detect it and NOT forward it
        // upstream...
        final SipResponse response = options.toRequest().createResponse(200).build();
        final SipResponseIOEvent responseEvent = IOEvent.create(connection, response);
        transportLayer.channelRead(defaultChannelCtx, responseEvent);
        defaultChannelCtx.assertSipMessageNotForwarded(response);
    }

    /**
     * If we end up in the PING state we will stay there until we receive something, either
     * a PONG or any other traffic. If we don't, then a timer will fire and we will re-issue
     * the PING and after a configurable amount of time we will give up and transition
     * to the CLOSING state, which will kill the flow.
     *
     * @throws Exception
     */
    @Test
    public void testSipOptionsPingNoResponseTimeout() throws Exception {
        initiateFlowToSipOptionsPing(getTransport());

    }


    /**
     * Convenience method for transitioning the Flow state machine over to waiting for a pong
     * @param transport
     * @throws Exception
     * @return first object, the connection, second, the SIP Options request
     *
     */
    public Object[] initiateFlowToSipOptionsPing(final Transport transport) throws Exception {
        final TransportLayerConfiguration config =
                loadConfiguration(SipConfiguration.class, "PingTest001.yaml").getTransport();
        final Connection connection = (Connection)initiateFlowToActive(config, transport)[0];

        defaultClock.plusSeconds(config.getFlow().getKeepAliveConfiguration().getIdleTimeout().plusSeconds(1).getSeconds());

        // Fire the idle timer, which then should trigger a SIP options to be sent out.
        // Note that the idle timer is scheduled as the Timeout2 timer
        defaultScheduler.fire(SipTimer.Timeout2);
        final SipRequestIOEvent optionsEvent = defaultChannelCtx.findWrittenMessageByType(SipRequestIOEvent.class);
        final SipRequest options = optionsEvent.request();

        // ensure that the options is indeed an options and that the
        // request-uri, to and from are all matching up that of the
        // configuration since the user can control this.
        final SipOptionsPingConfiguration sipOptionsConfig = config.getFlow().
                getKeepAliveConfiguration().
                getKeepAliveMethodConfiguration(transport).
                getSipOptionsConfiguration();
        assertThat(options.isOptions(), is(true));
        final SipURI actualFrom = options.getFromHeader().getAddress().getURI().toSipURI();
        final SipURI actualTo = options.getToHeader().getAddress().getURI().toSipURI();
        final String expectedFrom = sipOptionsConfig.getFromUser();
        final String expectedTo = sipOptionsConfig.getToUser();

        assertThat(actualFrom.getUser().toString(), is(expectedFrom));
        assertThat(actualTo.getUser().toString(), is(expectedTo));

        return new Object[]{connection, options};
    }
}
