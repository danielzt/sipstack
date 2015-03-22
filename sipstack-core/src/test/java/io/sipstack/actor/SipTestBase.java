/**
 * 
 */
package io.sipstack.actor;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.config.SipConfiguration;
import io.sipstack.event.Event;
import io.sipstack.event.SipEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipTestBase {

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

    public SipTestBase() {
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
     * Simple {@link Scheduler} that simply just saves all the events.
     * 
     * @author jonas
     *
     */
    public static class MockSheduler implements Scheduler {

        @Override
        public void scheduleUpstreamEventOnce(final Duration delay, final Event event) {
            // TODO Auto-generated method stub
        }

        @Override
        public void scheduleDownstreamEventOnce(final Duration delay, final Event event) {
            // TODO Auto-generated method stub
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
        public void onUpstreamEvent(final ActorContext ctx, final Event event) {
            this.upstreamEvents.add(event);
            if (event instanceof SipEvent) {
                final SipRequest request = ((SipEvent) event).getSipMessage().toRequest();
                for (final Integer responseStatus : this.responses) {
                    final SipResponse response = request.createResponse(responseStatus);
                    final SipEvent responseEvent = SipEvent.create(event.key(), response);
                    ctx.forwardDownstreamEvent(responseEvent);
                }
            }
            ctx.forwardUpstreamEvent(event);
        }

        @Override
        public void onDownstreamEvent(final ActorContext ctx, final Event event) {
            this.downstreamEvents.add(event);
            ctx.forwardDownstreamEvent(event);
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
