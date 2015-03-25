/**
 * 
 */
package io.sipstack.actor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.ActorSystem.DefaultActorSystem.DispatchJob;
import io.sipstack.config.SipConfiguration;
import io.sipstack.event.Event;
import io.sipstack.event.SipEvent;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.netty.codec.sip.SipMessageEvent;
import io.sipstack.netty.codec.sip.Transport;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipTestBase {

    /**
     * When kicking off new tests we usually send a SipMessage via the {@link ActorSystem} and when
     * doing so we need a {@link ConnectionId} as part of the {@link SipMessageEvent} (inderectly
     * via {@link Connection}). This is the default one if your test doesn't care which connection
     * the message came from.
     */
    protected ConnectionId defaultConnectionId;

    protected SipConfiguration sipConfig = new SipConfiguration();

    protected SipRequest invite;
    protected SipResponse ringing;
    protected SipResponse twoHundredToInvite;

    private final String inviteStr;
    private final String ringingStr;
    private final String twoHundredToInviteStr;
    private final String ackStr;
    private final String byeStr;
    private final String twoHundredByeStr;

    public SipTestBase() throws Exception {
        this.defaultConnectionId = createConnectionId(Transport.udp, "10.36.10.10", 5060, "192.168.0.100", 5060);

        StringBuilder sb = new StringBuilder();
        sb.append("INVITE sip:service@127.0.0.1:5060 SIP/2.0\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.1.1:5061;branch=z9hG4bK-25980-1-0\r\n");
        sb.append("From: sipp <sip:sipp@127.0.1.1:5061>;tag=25980SIPpTag001\r\n");
        sb.append("To: sut <sip:service@127.0.0.1:5060>\r\n");
        sb.append("Call-ID: 1-25980@127.0.1.1\r\n");
        sb.append("CSeq: 1 INVITE\r\n");
        sb.append("Contact: sip:sipp@127.0.1.1:5061\r\n");
        sb.append("Max-Forwards: 70\r\n");
        sb.append("Subject: Performance Test\r\n");
        sb.append("Content-Type: application/sdp\r\n");
        sb.append("Content-Length:   129\r\n");
        sb.append("\r\n");
        sb.append("v=0\r\n");
        sb.append("o=user1 53655765 2353687637 IN IP4 127.0.1.1\r\n");
        sb.append("s=-\r\n");
        sb.append("c=IN IP4 127.0.1.1\r\n");
        sb.append("t=0 0\r\n");
        sb.append("m=audio 6000 RTP/AVP 0\r\n");
        sb.append("a=rtpmap:0 PCMU/8000\r\n");
        this.inviteStr = sb.toString();

        sb = new StringBuilder();
        sb.append("SIP/2.0 180 OK\r\n");
        sb.append("From: sipp <sip:sipp@127.0.1.1:5061>;tag=25980SIPpTag001\r\n");
        sb.append("To: sut <sip:service@127.0.0.1:5060>\r\n");
        sb.append("Call-ID: 1-25980@127.0.1.1\r\n");
        sb.append("CSeq: 1 INVITE\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.1.1:5061;branch=z9hG4bK-25980-1-0\r\n");
        sb.append("Max-Forwards: 70\r\n");
        this.ringingStr = sb.toString();

        sb = new StringBuilder();
        sb.append("SIP/2.0 200 OK\r\n");
        sb.append("From: sipp <sip:sipp@127.0.1.1:5061>;tag=25980SIPpTag001\r\n");
        sb.append("To: sut <sip:service@127.0.0.1:5060>\r\n");
        sb.append("Call-ID: 1-25980@127.0.1.1\r\n");
        sb.append("CSeq: 1 INVITE\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.1.1:5061;branch=z9hG4bK-25980-1-0\r\n");
        sb.append("Max-Forwards: 70\r\n");
        this.twoHundredToInviteStr = sb.toString();


        sb = new StringBuilder();
        sb.append("ACK sip:service@127.0.0.1:5060 SIP/2.0\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.1.1:5061;branch=z9hG4bK-25980-1-5\r\n");
        sb.append("From: sipp <sip:sipp@127.0.1.1:5061>;tag=25980SIPpTag001\r\n");
        sb.append("To: sut <sip:service@127.0.0.1:5060>\r\n");
        sb.append("Call-ID: 1-25980@127.0.1.1\r\n");
        sb.append("CSeq: 1 ACK\r\n");
        sb.append("Contact: sip:sipp@127.0.1.1:5061\r\n");
        sb.append("Max-Forwards: 70\r\n");
        sb.append("Subject: Performance Test\r\n");
        sb.append("Content-Length: 0\r\n");
        this.ackStr = sb.toString();

        sb = new StringBuilder();
        sb.append("BYE sip:service@127.0.0.1:5060 SIP/2.0\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.1.1:5061;branch=z9hG4bK-25980-1-7\r\n");
        sb.append("From: sipp <sip:sipp@127.0.1.1:5061>;tag=25980SIPpTag001\r\n");
        sb.append("To: sut <sip:service@127.0.0.1:5060>\r\n");
        sb.append("Call-ID: 1-25980@127.0.1.1\r\n");
        sb.append("CSeq: 2 BYE\r\n");
        sb.append("Contact: sip:sipp@127.0.1.1:5061\r\n");
        sb.append("Max-Forwards: 70\r\n");
        sb.append("Subject: Performance Test\r\n");
        sb.append("Content-Length: 0\r\n");
        this.byeStr = sb.toString();

        sb = new StringBuilder();
        sb.append("SIP/2.0 200 OK\r\n");
        sb.append("From: sipp <sip:sipp@127.0.1.1:5061>;tag=25980SIPpTag001\r\n");
        sb.append("To: sut <sip:service@127.0.0.1:5060>\r\n");
        sb.append("Call-ID: 1-25980@127.0.1.1\r\n");
        sb.append("CSeq: 2 BYE\r\n");
        sb.append("Via: SIP/2.0/UDP 127.0.1.1:5061;branch=z9hG4bK-25980-1-7\r\n");
        sb.append("Max-Forwards: 70\r\n");
        this.twoHundredByeStr = sb.toString();
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        this.invite = SipMessage.frame(this.inviteStr).toRequest();
        this.ringing = SipMessage.frame(this.ringingStr).toResponse();
        this.twoHundredToInvite = SipMessage.frame(this.twoHundredToInviteStr).toResponse();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {}

    /**
     * Helper method for creating a new {@link ConnectionId} object.
     */
    public ConnectionId createConnectionId(final Transport transport, final String localIp, final int localPort,
            final String remoteIp, final int remotePort) throws Exception {
        final InetSocketAddress local = new InetSocketAddress(localIp, localPort);
        final InetSocketAddress remote = new InetSocketAddress(remoteIp, remotePort);
        return ConnectionId.create(transport, local, remote);
    }

    public SipMessageEvent mockSipMessageEvent(final SipMessage msg) {
        final SipMessageEvent event = mock(SipMessageEvent.class);
        final Connection connection = mockConnection(this.defaultConnectionId);
        // TODO: should be our internal fake clock
        when(event.getArrivalTime()).thenReturn(System.currentTimeMillis());
        when(event.getConnection()).thenReturn(connection);
        when(event.getMessage()).thenReturn(msg);
        return event;
    }

    /**
     * Helper method for creating a mocked {@link Connection} based off of a {@link ConnectionId}.
     * 
     * @param connectionId
     * @return
     */
    public Connection mockConnection(final ConnectionId connectionId) {
        final Connection connection = mock(Connection.class);
        when(connection.id()).thenReturn(connectionId);
        when(connection.getTransport()).thenReturn(connectionId.getProtocol());
        when(connection.getLocalIpAddress()).thenReturn(connectionId.getLocalIpAddress());
        when(connection.getLocalPort()).thenReturn(connectionId.getLocalPort());
        when(connection.getRemoteIpAddress()).thenReturn(connectionId.getRemoteIpAddress());
        when(connection.getRemotePort()).thenReturn(connectionId.getRemotePort());
        return connection;
    }


    /**
     * Simple {@link Timer} that simply just saves all the events so that we can examine them later
     * and also "fire" them off..
     * 
     * @author jonas@jonasborjesson.com
     *
     */
    public static class MockTimer implements Timer {
        public final List<TimerTaskSnapshot> tasks = new ArrayList<TimerTaskSnapshot>();

        @Override
        public Timeout newTimeout(final TimerTask task, final long delay, final TimeUnit unit) {
            final Timeout timeout = mock(Timeout.class);
            this.tasks.add(new TimerTaskSnapshot(task, delay, unit, timeout));
            return timeout;
        }

        @Override
        public Set<Timeout> stop() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    /**
     * Simple class just to keep all the parameters together when it comes to the timer task that
     * was scheduled on a {@link Timer}.
     * 
     */
    public static class TimerTaskSnapshot {
        public final TimerTask task;
        public final long delay;
        public final TimeUnit unit;
        public final Timeout timeout;

        private TimerTaskSnapshot(final TimerTask task, final long delay, final TimeUnit unit, final Timeout timeout) {
            this.task = task;
            this.delay = delay;
            this.unit = unit;
            this.timeout = timeout;
        }
    }

    public static class DelayedJob {
        public final Duration delay;
        public final DispatchJob job;

        private DelayedJob(final Duration delay, final DispatchJob job) {
            this.delay = delay;
            this.job = job;
        }
    }

    public class MockActorSystem implements ActorSystem {
        public final List<DelayedJob> scheduledJobs = new ArrayList<>();
        public final List<DispatchJob> jobs = new ArrayList<>();

        private final PipeLineFactory pipeLineFactory;

        public MockActorSystem(final PipeLineFactory pipeLineFactory) {
            this.pipeLineFactory = pipeLineFactory;
        }

        @Override
        public void receive(final SipMessageEvent event) {
            // TODO Auto-generated method stub
        }

        @Override
        public Timeout scheduleJob(final Duration delay, final DispatchJob job) {
            this.scheduledJobs.add(new DelayedJob(delay, job));
            return mock(Timeout.class);
        }

        @Override
        public void dispatchJob(final DispatchJob job) {
            this.jobs.add(job);
        }

        @Override
        public DispatchJob createJob(final Direction direction, final Event event, final PipeLine pipeLine) {
            return new DispatchJob(this, 0, pipeLine, event, direction);
        }

        @Override
        public DispatchJob createJob(final Direction direction, final Event event) {
            final PipeLine pipeLine = this.pipeLineFactory.newPipeLine();
            return new DispatchJob(this, 0, pipeLine, event, direction);
        }

        @Override
        public Actor actorOf(final Key key) {
            // TODO Auto-generated method stub
            return null;
        }

    }


    /**
     * Simple {@link Actor} that simply just forwards the message in the same direction it came and
     * saves all the events it has seen.
     * 
     * @author jonas
     *
     */
    public static class EventProxy implements Actor {

        public final List<Event> upstreamEvents = new ArrayList<Event>();
        public final List<Event> downstreamEvents = new ArrayList<Event>();

        List<Integer> responses;

        public EventProxy(final Integer... responses) {
            if (responses != null && responses.length > 0) {
                this.responses = Arrays.asList(responses);
            } else {
                this.responses = Collections.emptyList();
            }
        }

        @Override
        public void onEvent(final ActorContext ctx, final Event event) {
            this.upstreamEvents.add(event);
            if (event instanceof SipEvent) {
                final SipRequest request = ((SipEvent) event).getSipMessage().toRequest();
                for (final Integer responseStatus : this.responses) {
                    final SipResponse response = request.createResponse(responseStatus);
                    final SipEvent responseEvent = SipEvent.create(event.key(), response);
                    ctx.reverse().forward(responseEvent);
                }
            }
            ctx.forward(event);
        }

        public void reset() {
            this.upstreamEvents.clear();
            this.downstreamEvents.clear();
        }

        @Override
        public Supervisor getSupervisor() {
            // TODO Auto-generated method stub
            return null;
        }
    }



}
