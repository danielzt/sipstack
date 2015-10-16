package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.header.CallIdHeader;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.Transport;
import io.sipstack.transaction.ClientTransaction;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.event.SipTransactionEvent;
import io.sipstack.transaction.event.TransactionEvent;
import io.sipstack.transport.Flow;
import io.sipstack.transport.event.FlowEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author jonas@jonasborjesson.com
 */
public class NonInviteClientTransactionActorTest extends TransactionTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test to send a non-invite request
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testTrying() throws Exception {
        // TODO: we should send every type of request there is.
        final SipTransactionEvent holder = initiateNewTransaction("bye");
    }

    /**
     * While in the trying state, ensure that whenever timer E fires
     * that we indeed re-transmit the request.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testRetransmitRequestWhileInTrying() throws Exception {
        initiateNewTransaction("bye");
        defaultScheduler.fire(SipTimer.E);

        transports.assertRequest("bye");

        // timer E should have been re-scheduled.
        assertTimerScheduled(SipTimer.E);
    }

    /**
     * If timer F fires in trying we will end up in the terminated state.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testFireTimerFWhileInTrying() throws Exception {
        final Transaction transaction = initiateNewTransaction("bye").transaction();
        defaultScheduler.fire(SipTimer.F);
        assertTimerCancelled(SipTimer.E);
        mockChannelContext.ensureTransactionTerminated(transaction.id());
    }

    /**
     * Whenever we get a final response while in the Trying state we should transition over
     * to the completed state.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testTryingToCompleted() throws Exception {
        transitionFromTryingToCompleted("bye", 200);
    }

    /**
     * For unreliable transports we will get "stuck" in the COMPLETED
     * state in order to consume re-transmissions and when timer K fires
     * we will move over to TERMINATED so ensure that happens.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testTransitionToTerminatedFromCompleted() throws Exception {
        final SipTransactionEvent holder = transitionFromTryingToCompleted("bye", 200);
        defaultScheduler.fire(SipTimer.K);
        mockChannelContext.ensureTransactionTerminated(holder.transaction().id());
    }

    /**
     * Ensure we can go the path Trying -> Proceeding -> Completed
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testTransitionToCompletedFromProceeding() throws Exception {
        // TODO: test all combinations of provisional and all known SIP methods
        final SipTransactionEvent holder = transitionFromTryingToProceeding("bye", 100);
    }

    /**
     * Test the full transition between:
     * Trying -> Proceeding -> Completed -> Terminated
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testTransitionTryingProceedingCompletedTerminated() throws Exception {
        final SipTransactionEvent holder = transitionFromTryingToProceeding("bye", 100);
        final SipRequest request = holder.message().toRequest();
        final SipResponse response = request.createResponse(200).build();

        final Flow flow = mock(Flow.class);
        transactionLayer.channelRead(mockChannelContext, FlowEvent.create(flow, response));
        mockChannelContext.assertAndConsumeResponse("bye", 200);

        // should now be in completed so timer K should have been
        // scheduled
        assertTimerScheduled(SipTimer.K);

        // fire timer K which then takes us to terminated
        defaultScheduler.fire(SipTimer.K);
        mockChannelContext.ensureTransactionTerminated(holder.transaction().id());
    }

    /**
     * While in proceeding, any provisional responses should be passed to the TU
     * and that's it...
     */
    @Test(timeout = 500)
    public void test1xxResponsesWhileInProceedingState() throws Exception {
        final SipTransactionEvent holder = transitionFromTryingToProceeding("bye", 100);
        final SipRequest request = holder.message().toRequest();
        final SipResponse response = request.createResponse(180).build();
        final Flow flow = mock(Flow.class);
        transactionLayer.channelRead(mockChannelContext, FlowEvent.create(flow, response));
        mockChannelContext.assertAndConsumeResponse("bye", 180);
    }

    /**
     * Timer E will always control re-transmission so ensure this is true in
     * the proceeding state as well.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testTimerEWhileInProceedingState() throws Exception {
        final SipTransactionEvent holder = transitionFromTryingToProceeding("bye", 100);
        defaultScheduler.fire(SipTimer.E);

        transports.assertRequest("bye");

        // timer E should have been re-scheduled.
        assertTimerScheduled(SipTimer.E);
    }

    /**
     * Timer F will always take us over to the terminated state no matter
     * which state we are in. In this case, we are in proceeding so ensure
     * that we will transition over to the TERMINATED state.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testTimerFWhileInProceedingState() throws Exception {
        final SipTransactionEvent holder = transitionFromTryingToProceeding("bye", 100);
        defaultScheduler.fire(SipTimer.F);
        assertTimerCancelled(SipTimer.E);
        mockChannelContext.ensureTransactionTerminated(holder.transaction().id());
    }



    /**
     * Helper method to transition Trying -> Completed.
     *
     * @param responseCode
     * @return
     * @throws Exception
     */
    private SipTransactionEvent transitionFromTryingToCompleted(final String method, final int responseStatus) throws Exception {
        // ensure no one is using this method wrong
        assertThat("Only final responses pls!", responseStatus >= 200, is(true));

        final SipTransactionEvent holder = initiateNewTransaction(method);
        final SipRequest request = holder.message().toRequest();
        final SipResponse response = request.createResponse(responseStatus).build();
        final Flow flow = mock(Flow.class);
        transactionLayer.channelRead(mockChannelContext, FlowEvent.create(flow, response));
        mockChannelContext.assertAndConsumeResponse(method, responseStatus);

        assertTimerScheduled(SipTimer.K);
        return holder;
    }

    /**
     * Helper method to transition Trying -> Proceeding
     */
    private SipTransactionEvent transitionFromTryingToProceeding(final String method, final int responseStatus) throws Exception {
        // ensure no one is using this method wrong
        assertThat("Only provisional responses pls!", responseStatus / 100 == 1, is(true));

        final SipTransactionEvent holder = initiateNewTransaction(method);
        final SipRequest request = holder.message().toRequest();
        final SipResponse response = request.createResponse(responseStatus).build();
        final Flow flow = mock(Flow.class);
        transactionLayer.channelRead(mockChannelContext, FlowEvent.create(flow, response));
        mockChannelContext.assertAndConsumeResponse(method, responseStatus);

        assertTimerScheduled(SipTimer.E);
        assertTimerScheduled(SipTimer.F);
        return holder;
    }

    /**
     * Initiate a new non-invite client transaction returning both the transaction and the request
     * as it was propagated through the transaction layer.
     *
     * This method will also ensure that the state is in the TRYING state and that
     * the timers E & F has properly been scheduled.
     *
     * @return
     * @throws Exception
     */
    public SipTransactionEvent initiateNewTransaction(final String method) throws Exception {
        final SipRequest request = generateRequest(method);

        // change the branch since we will otherwise actually hit the
        // same transaction and then this will be a re-transmission
        // instead which is not what we want.
        request.setHeader(CallIdHeader.create());
        final ViaHeader via = request.getViaHeader().copy().withBranch(ViaHeader.generateBranch()).build();
        request.setHeader(via);

        final AtomicReference<Transaction> tRef = new AtomicReference<>();
        transactionLayer.createFlow("127.0.0.1")
                .withPort(5070)
                .withTransport(Transport.udp)
                .onSuccess(f -> {
                    final ClientTransaction transaction = transactionLayer.newClientTransaction(f, request);
                    tRef.set(transaction);
                    transaction.start();
                })
                .onFailure(f -> fail("Creating the flow shouldnt fail"))
                .onCancelled(f -> fail("The flow shouldn't have been cancelled"))
                .connect();

        // the request should have been sent through the transaction layer down to the
        // transport layer, for which we have a mock implementation so check that we
        // received it there.
        final FlowEvent event = mockChannelContext.assertAndConsumeDownstreamRequest(method);
        final SipRequest request2 = event.toSipRequestFlowEvent().request();
        assertThat(request, is(request2));

        assertTimerScheduled(SipTimer.E);
        assertTimerScheduled(SipTimer.F);

        return TransactionEvent.create(tRef.get(), request2);
    }

    private SipRequest generateRequest(final String method) {
        if ("BYE".equalsIgnoreCase(method)) {
            return defaultByeRequest;
        }

        fail("I am not handling that method right now. Add above...");
        return null;
    }

}