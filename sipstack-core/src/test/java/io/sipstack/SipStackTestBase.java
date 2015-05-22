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
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


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

        // configure the dispatcher
        // final WorkerThreadExecutorConfig workerConfig = (new WorkerThreadExecutorConfig.Builder()).withNoOfWorkers(4).build();
        // final Builder dispatchBuilder =  new Builder();
        // dispatchBuilder.withExecutor("worker-thread-executor").withThroughput(4).withWorkerThreadExecutor(workerConfig);
        // final Map<String, DispatcherConfiguration> dispatchers = new HashMap<>();
        // final DispatcherConfiguration defaultDispatcher = dispatchBuilder.build();
        // hektorConfig.dispatchers(dispatchers);



        hektor = Hektor.withName("hello").withConfiguration(hektorConfig).build();
        actor = Application.ActorSystemBuilder.withConfig(config).withHektor(hektor).build();
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
}
