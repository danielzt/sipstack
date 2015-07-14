package io.sipstack;

import io.netty.channel.ChannelHandlerContext;
import io.pkts.Pcap;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipPacket;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.streams.SipStream;
import io.pkts.streams.Stream;
import io.pkts.streams.StreamHandler;
import io.pkts.streams.StreamListener;
import io.pkts.streams.impl.DefaultStreamHandler;
import io.sipstack.net.InboundOutboundHandlerAdapter;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.netty.codec.sip.SipMessageEvent;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.Transport;
import org.junit.Before;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
 * Test base for most of the tests within sipstack.io.
 *
 * @author jonas@jonasborjesson.com
 */
public class SipStackTestBase {

    /**
     * Default requests and responses that belongs to the same dialog. These
     * can be used to push requests/responses through the stack.
     */
    protected SipRequest defaultInviteRequest;
    protected SipResponse defaultInvite200Response;
    protected SipRequest defaultAckRequest;
    protected SipRequest defaultByeRequest;
    protected SipResponse defaultBye200Response;

    protected MockConnection defaultConnection;

    /**
     * A mock implementation of the netty {@link ChannelHandlerContext}
     *
     */
    protected MockChannelHandlerContext defaultChannelCtx;

    protected MockScheduler defaultScheduler;

    /**
     * All known provisional responses in SIP
     */
    protected static final int[] PROVISIONAL = new int[]{100, 180, 181, 182, 183, 199};

    /**
     * All known successful responses in SIP
     */
    protected static final int[] SUCCESSFUL = new int[]{200, 202, 204};

    /**
     * All known redirect responses in SIP
     */
    protected static final int[] REDIRECT = new int[]{300, 301, 302, 305, 380};

    /**
     * All known client error responses in SIP
     */
    protected static final int[] CLIENT_FAILURES = new int[]{400, 401, 402, 403, 404, 405, 406, 407,
            408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 420, 421, 422, 423, 424, 428, 429, 430,
            433, 436, 437, 438, 439, 470, 480, 481, 482, 483, 484, 485, 486, 487, 488, 489, 491, 493, 494};

    /**
     * All known server error responses in SIP
     */
    protected static final int[] SERVER_FAILURES = new int[]{500, 501, 502, 503, 504, 505, 513, 580};

    /**
     * All known global error responses in SIP
     */
    protected static final int[] GLOBAL_FAILURES = new int[]{600, 603, 604, 606};

    @Before
    public void setUp() throws Exception {

        // load a basic call and store the individual messages
        // for later use within tests...
        final SipStream stream = loadSipStream("simple_call.pcap");
        defaultInviteRequest = findRequest(stream, pkt -> pkt.isRequest() && pkt.isInvite());
        defaultInvite200Response = findResponse(stream, pkt -> pkt.isResponse() && pkt.isInvite() && pkt.toResponse().getStatus() / 100 == 2);
        defaultByeRequest = findRequest(stream, pkt -> pkt.isRequest() && pkt.isBye());
        defaultAckRequest = findRequest(stream, pkt -> pkt.isRequest() && pkt.isAck());
        defaultBye200Response = findResponse(stream, pkt -> pkt.isResponse() && pkt.isBye() && pkt.toResponse().getStatus() / 100 == 2);

        final ConnectionId id = createConnectionId(Transport.udp, "10.36.10.100", 5060, "192.168.0.100", 5090);
        defaultConnection = new MockConnection(id);
        defaultScheduler = new MockScheduler(new CountDownLatch(1));
    }

    /**
     * it is quite often easier to e.g. send one message through the pipe, assert
     * that message was handled as it should and then reset the context
     * again so we start over.
     */
    public void resetChannelHandlerContext(final InboundOutboundHandlerAdapter handler) {
        defaultChannelCtx.reset(handler);
    }

    public void resetChannelHandlerContext() {
        defaultChannelCtx.reset();
    }

    /**
     * Convenience method for creating a new connection id.
     */
    public ConnectionId createConnectionId(final Transport protocol, final String localIp, final int localPort,
                                            final String remoteIp, final int remotePort) {
        final InetSocketAddress localAddress = new InetSocketAddress(localIp, localPort);
        final InetSocketAddress remoteAddress = new InetSocketAddress(remoteIp, remotePort);
        return ConnectionId.create(protocol, localAddress, remoteAddress);
    }

    public SipMessageEvent createEvent(final SipMessage msg) {
        return new SipMessageEvent(defaultConnection, msg, 0);
    }


    protected SipRequest findRequest(final SipStream stream, final Predicate<SipPacket> predicate) {
        return findMessage(stream, predicate).toRequest();
    }

    protected SipResponse findResponse(final SipStream stream, final Predicate<SipPacket> predicate) {
        return findMessage(stream, predicate).toResponse();
    }

    protected SipMessage findMessage(final SipStream stream, final Predicate<SipPacket> predicate) {
        return stream.getPackets().stream().filter(predicate).map(pkt -> {
            try {
                return SipMessage.frame(pkt.toBuffer());
            } catch (final IOException e) {
                return null;
            }
        }).findFirst().get();
    }

    protected List<SipStream> loadSipStreams(final String resource) throws Exception {
        final Pcap pcap = Pcap.openStream(SipStackTestBase.class.getResourceAsStream(resource));
        final StreamHandler streamHandler = new DefaultStreamHandler();
        final List<SipStream> streams = new ArrayList<>();

        streamHandler.addStreamListener(new StreamListener<SipPacket>() {

            @Override
            public void startStream(final Stream<SipPacket> stream, final SipPacket packet) {
                streams.add((SipStream)stream);
            }

            @Override
            public void packetReceived(final Stream<SipPacket> stream, final SipPacket packet) {
            }

            @Override
            public void endStream(final Stream<SipPacket> stream) {
            }
        });

        pcap.loop(streamHandler);
        return streams;
    }

    protected SipStream loadSipStream(final String resource) throws Exception {
        final List<SipStream> streams = loadSipStreams(resource);
        if (streams.size() == 1) {
            return streams.get(0);
        }

        throw new RuntimeException("Only expected a single stream in the pcap");
    }

    /**
     * There are a lot of various sip timers that are being scheduled and this
     * method helps you ensure that a particular timer was indeed scheduled.
     *
     * @param timer
     * @return the cancellable that we just asserted actually exists and is correct.
     */
    public MockCancellable assertTimerScheduled(final SipTimer timer) throws InterruptedException {
        defaultScheduler.latch.await();
        final Optional<MockCancellable> cancellable = defaultScheduler.isScheduled(timer);
        if (!cancellable.isPresent()) {
            fail("No timer " + timer + " scheduled");
        }
        return cancellable.get();
    }

    public MockCancellable assertTimerCancelled(final SipTimer timer) throws InterruptedException {
        MockCancellable cancellable = assertTimerScheduled(timer);
        assertThat("Timer " + timer + " was not cancelled as expected", cancellable.isCancelled(), is(true));
        return cancellable;
    }

}
