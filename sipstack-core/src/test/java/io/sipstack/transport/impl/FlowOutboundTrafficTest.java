package io.sipstack.transport.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.Transport;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.header.CallIdHeader;
import io.pkts.packet.sip.header.ContactHeader;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.config.SipConfiguration;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.SipRequestBuilderIOEvent;
import io.sipstack.transport.Flow;
import io.sipstack.transport.event.FlowEvent;
import io.sipstack.transport.event.SipRequestBuilderFlowEvent;
import io.sipstack.transport.event.SipResponseBuilderFlowEvent;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * These set of tests focuses on outbound traffic where the main thing is
 * to actually update the Via- and Contact-header with the necessary
 * information, assuming the user doesn't believe she knows better. I.e.,
 * when the a request is asked to be sent, i.e. it is coming down the
 * netty pipeline as a "write event" the flow is responsible for filling
 * out the Via-header with the correct IP/Port/Transport since it is
 * really the only one that knows.
 *
 * Note, the design is that a user of this SIP stack must be able to say
 * "nope, I want to do that myself because I know what I'm doing" and since
 * sip messages are immutable we will simply on set the correct values if
 * we get a sip message builder through the flow. If we get an already
 * built sip message the flow assumes that you know what you are doing.
 *
 * @author jonas@jonasborjesson.com
 */
public abstract class FlowOutboundTrafficTest extends TransportLayerTestBase {

    private TransportLayerConfiguration config;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        config = loadConfiguration(SipConfiguration.class, "OutboundTrafficTest001.yaml").getTransport();
    }

    /**
     * We may be configured to use a VIP address and if so we should be using
     * it as opposed to the real one.
     *
     * @throws Exception
     */
    @Test
    public void testStampingVipAddressInContact() throws Exception {
        defaultVipAddress = SipURI.withHost("hello.com").withPort(1122).build();
        final Transport transport = getTransport();
        final SipMessage msg = initiateOutboundMessage(transport, config);
        final ContactHeader contactHeader = msg.getContactHeader();
        assertThat(contactHeader.getAddress().getURI().toSipURI().getHost().toString(), is("hello.com"));
        assertThat(contactHeader.getAddress().getURI().toSipURI().getPort(), is(1122));
    }

    /**
     * The Contact header should be updated with the ip:port & transport of flow.
     * @throws Exception
     */
    @Test
    public void testContactHeader() throws Exception {
        final Transport transport = getTransport();
        final SipMessage msg = initiateOutboundMessage(transport, config);
        final ContactHeader contactHeader = msg.getContactHeader();
        assertThat(contactHeader.getAddress().getURI().toSipURI().getHost().toString(), is(defaultLocalIPAddress));
        assertThat(contactHeader.getAddress().getURI().toSipURI().getPort(), is(defaultLocalPort));
        assertThat(contactHeader.getAddress().getURI().toSipURI().getTransportParam().get(), is(transport));
    }

    /**
     * If the default port for the transport is specified then it shouldn't be stamped on the
     * Contact header. I.e. there should be a contact like so:
     *
     * <sip:sipp@192.168.0.100:5060;transport=tcp>
     *
     * but rather 5060 should be left out like so
     *
     * <sip:sipp@192.168.0.100;transport=tcp>
     *
     * Doesn't really matter per se but I hate when stacks stamp un-necessary things. I almost
     * hate it as much as when I see stacks stamping "lr=on" in Route-headers!!!
     *
     * @throws Exception
     */
    @Test
    public void testContactHeaderDontSetDefaultPort() throws Exception {
        defaultLocalPort = 5060;
        final Transport transport = getTransport();
        final SipMessage msg = initiateOutboundMessage(transport, config);
        final ContactHeader contactHeader = msg.getContactHeader();

        // negative 1 signifies that the port isn't set.
        assertThat(contactHeader.getAddress().getURI().toSipURI().getPort(), is(-1));
    }

    /**
     * Ensure that we fill out the IP:Port:Transport correctly.
     *
     * @throws Exception
     */
    @Test
    public void testOutboundRequestViaHostPortIp() throws Exception {
        final Transport transport = getTransport();
        final SipMessage msg = initiateOutboundMessage(transport, config);

        final ViaHeader finalVia = msg.getViaHeader();
        assertThat(finalVia.getTransport(), is(transport.toUpperCaseBuffer()));
        assertThat(finalVia.getHost().toString(), is(defaultLocalIPAddress));
        assertThat(finalVia.getPort(), is(defaultLocalPort));
    }

    /**
     * For a SIP request we have to two choices, either push the rport onto the Via
     * (as a flag parameter) or not. This is controlled by configuration.
     *
     * @throws Exception
     */
    @Test
    public void testOutboundRequestViaRport() throws Exception {
        final Transport transport = getTransport();

        // first force the rport
        config.setPushRPort(true);
        SipMessage msg = initiateOutboundMessage(transport, config);

        ViaHeader finalVia = msg.getViaHeader();
        assertThat(finalVia.hasRPort(), is(true));
        assertThat(finalVia.getRPort(), is(-1));
        assertThat(finalVia.toString().contains("rport"), is(true));

        // then turn it off
        config.setPushRPort(false);
        msg = initiateOutboundMessage(transport, config);

        finalVia = msg.getViaHeader();
        assertThat(finalVia.hasRPort(), is(false));
        assertThat(finalVia.getRPort(), is(-1));
        // there was a bug in the Via header implementation where
        // even though the actual Via header object reported that
        // it did not have an rport, the actual buffer as written
        // out to a stream actually did so make sure we dont
        // have an rport!!!
        assertThat(finalVia.toString().contains("rport"), is(false));
    }

    public SipMessage initiateOutboundMessage(final Transport transport,
                                              final TransportLayerConfiguration config) throws Exception {
        final SipMessage.Builder<SipRequest> builder = defaultInviteRequest.copy();
        return initiateOutboundMessage(transport, config, builder);
    }

    public SipMessage initiateOutboundMessage(final Transport transport,
                                              final TransportLayerConfiguration config,
                                              final SipMessage.Builder<SipRequest> builder) throws Exception {
        final Object[] objects = initiateFlowToActive(config, transport);
        final Connection connection = (Connection)objects[0];
        final Flow flow = (Flow) objects[1];

        final FlowEvent event = builder.isSipRequestBuilder() ? SipRequestBuilderFlowEvent.create(flow, builder.toSipRequestBuilder()) :
                SipResponseBuilderFlowEvent.create(flow, builder.toSipResponseBuilder());
        transportLayer.write(defaultChannelCtx, event, null);
        final SipRequestBuilderIOEvent builderEvent =
                defaultChannelCtx.findWrittenMessageByType(SipRequestBuilderIOEvent.class);

        // Also note that the Transport layer will NOT actually build the
        // request since that should ONLY be done by the component actually
        // sending the message. I.e., only at the very last minute is the message
        // turned into a SipMessage and then turned into a byte[].
        // Therefore, we are issuing the build as part of the test.
        return builderEvent.getBuilder().build();
    }

    public abstract Transport getTransport();

}
