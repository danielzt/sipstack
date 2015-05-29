package io.sipstack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.hektor.config.HektorConfiguration;
import io.hektor.core.ActorRef;
import io.hektor.core.Hektor;
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
import io.sipstack.config.Configuration;
import io.sipstack.core.Application;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.netty.codec.sip.Transport;
import io.sipstack.timers.SipTimer;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;


/**
 * Test base for most of the tests within sipstack.io.
 *
 * @author jonas@jonasborjesson.com
 */
public class SipStackTestBase {

    protected Hektor hektor;

    /**
     * The actor that is the entry into the entire stack.
     */
    protected ActorRef actor;

    /**
     * This is the scheduler we will use, which will not actually schedule anything :-)
     */
    protected MockScheduler scheduler;

    protected MockConnection connection;

    /**
     * Default requests and responses that belongs to the same dialog. These
     * can be used to push requests/responses through the stack.
     */
    protected SipRequest defaultInviteRequest;
    protected SipResponse defaultInvite200Response;
    protected SipRequest defaultAckRequest;
    protected SipRequest defaultByeRequest;
    protected SipResponse defaultBye200Response;

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


        // configure the default actor system.
        final Configuration config = new Configuration();
        // final HektorConfiguration hektorConfig = config.getHektorConfiguration();
        final HektorConfiguration hektorConfig = loadConfiguration("default_hektor_config.yaml");

        scheduler = new MockScheduler(new CountDownLatch(1));
        hektor = Hektor.withName("hello").withConfiguration(hektorConfig).withScheduler(scheduler).build();
        actor = Application.ActorSystemBuilder.withConfig(config).withHektor(hektor).build();

        final ConnectionId id = createConnectionId(Transport.udp, "10.36.10.100", 5060, "192.168.0.100", 5090);
        connection = new MockConnection(id);
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



    public  HektorConfiguration loadConfiguration(final String resource) throws Exception {
        final InputStream stream = SipStackTestBase.class.getResourceAsStream(resource);
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final SimpleModule module = new SimpleModule();
        return mapper.readValue(stream, HektorConfiguration.class);
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

    @After
    public void tearDown() throws Exception {
    }


    /**
     * Check so that the expected response indeed was sent. This method assumes that the default connection
     * object is being used and that the response that you are expected is the first one in the sent list as stored
     * in the mock connection.
     *
     * @param method
     * @param status
     */
    protected void assertResponse(final String method, int status) throws InterruptedException {
        // wait until the latch releases which then indicates
        // that we have received at the expected response(s).
        connection.latch().await();
        final SipMessage msg = connection.consume();
        assertThat(msg.getMethod().toString(), is(method));
        assertThat(msg.toResponse().getStatus(), is(status));
    }

    /**
     * There are a lot of various sip timers that are being scheduled and this
     * method helps you ensure that a particular timer was indeed scheduled.
     *
     * @param timer
     * @return the cancellable that we just asserted actually exists and is correct.
     */
    protected MockCancellable assertTimerScheduled(final SipTimer timer) throws InterruptedException {
        scheduler.latch.await();
        return scheduler.isScheduled(timer).orElseThrow(RuntimeException::new);
    }
}
