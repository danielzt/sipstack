package io.sipstack.netty.codec.sip;

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
import org.junit.Before;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;


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
        defaultChannelCtx = new MockChannelHandlerContext(handler);
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
        return defaultScheduler.isScheduled(timer).orElseThrow(RuntimeException::new);
    }

}
