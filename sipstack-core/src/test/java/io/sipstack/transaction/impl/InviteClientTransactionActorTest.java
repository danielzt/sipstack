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
import io.sipstack.transport.event.SipFlowEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
 * @author jonas@jonasborjesson.com
 */
public class InviteClientTransactionActorTest extends TransactionTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Ack doesn't go in the same transaction but let's make sure it works...
     *
     * @throws Exception
     */
    @Test
    public void testSendAckOn200() throws Exception {
        final SipTransactionEvent event = initiateTransition(200);
        transports.assertAndConsumeRequest("ack");
    }

    /**
     * While in the accepted state, any 2xx responses will simply be forwarded
     * up to the TU.
     *
     * @throws Exception
     */
    // @Test(timeout = 1000)
    @Test
    public void testRetransmittedWhileInAccepted() throws Exception {
        final SipTransactionEvent event = initiateTransition(200);
        reset();
        final SipResponse reTransmitted = event.message().createResponse(200).build();
        final Flow flow = Mockito.mock(Flow.class);
        transactionLayer.channelRead(mockChannelContext, FlowEvent.create(flow, reTransmitted));
        mockChannelContext.assertAndConsumeResponse("invite", 200);
    }
    /**
     * Test to go straight from Calling to Accepted and then fire timer M which takes
     * us to the terminated state.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testTransitionToAccepted() throws Exception {
        for (int i = 0; i < SUCCESSFUL.length; ++i) {
            final Transaction transaction = initiateTransition(SUCCESSFUL[i]).transaction();
            assertTimerCancelled(SipTimer.A);
            assertTimerCancelled(SipTimer.B);
            assertTimerScheduled(SipTimer.M);

            // fire M
            defaultScheduler.fire(SipTimer.M);
            mockChannelContext.ensureTransactionTerminated(transaction.id());
            reset();
        }
    }

    /**
     * Test to transition Calling -> Completed
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testTransitionCallingToCompleted() throws Exception {

        final int provisionalResponse = PROVISIONAL[0];
        final int failureResponse = CLIENT_FAILURES[0];

        final SipTransactionEvent event = initiateTransition(provisionalResponse);
        final SipResponse response = event.message().createResponse(failureResponse).build();
        final Flow flow = Mockito.mock(Flow.class);
        transactionLayer.channelRead(mockChannelContext, FlowEvent.create(flow, response));

        assertTimerScheduled(SipTimer.D);
        reset();

    }

    /**
     * Test to go Calling --> Proceeding --> Accepted for every combination
     * of provisional and success responses.
     */
    @Test(timeout = 1000)
    public void testProceedingToAccepted() throws Exception {

        for (int i = 0; i < PROVISIONAL.length; ++i) {
            for (int j = 0; j < SUCCESSFUL.length; ++j) {
                final int provisionalResponse = PROVISIONAL[i];
                final int successfulResponse = SUCCESSFUL[j];
                System.out.println("Testing provisional " + provisionalResponse + " then final " + successfulResponse);
                final SipTransactionEvent event = initiateTransition(provisionalResponse);
                final SipResponse response = event.request().createResponse(successfulResponse).build();
                final Flow flow = Mockito.mock(Flow.class);
                transactionLayer.channelRead(mockChannelContext, FlowEvent.create(flow, response));

                assertTimerScheduled(SipTimer.M);
                reset();
            }
        }
    }

    /**
     * Ensure that if we keep getting 1xx responses sent to us while in
     * the proceeding state that those gets pushed up to the Transaction User
     * layer.
     *
     * @throws Exception
     */
    @Test(timeout = 1000)
    public void testRetransmitWhileInProceeding() throws Exception {


        for (int i = 0; i < PROVISIONAL.length; ++i) {
            final SipTransactionEvent event = initiateTransition(PROVISIONAL[i]);
            assertTimerCancelled(SipTimer.A);
            assertTimerCancelled(SipTimer.B);
            reset();

            final SipResponse response = event.message().createResponse(i).build();
            final Flow flow = Mockito.mock(Flow.class);
            transactionLayer.channelRead(mockChannelContext, FlowEvent.create(flow, response));

            mockChannelContext.assertAndConsumeResponse("invite", i);
            reset();
        }
    }

    @Test(timeout = 1000)
    public void testTransitionToProceeding() throws Exception {
        for (int i = 100; i < 200; ++i) {
            initiateTransition(i);
            assertTimerCancelled(SipTimer.A);
            assertTimerCancelled(SipTimer.B);
            reset();
        }
    }

    @Test(timeout = 500)
    public void testFireTimerA() throws Exception {
        final SipTransactionEvent event = initiateNewTransaction();
        final Transaction t1 = event.transaction();
        final SipRequest request = event.message().toRequest();

        defaultScheduler.fire(SipTimer.A);

        final FlowEvent flowEvent = mockChannelContext.assertAndConsumeDownstreamRequest("invite");
        final SipRequest retransmittedRequest = flowEvent.toSipRequestFlowEvent().request();
        assertThat(request, is(retransmittedRequest));
    }

    @Test(timeout = 500)
    public void testFireTimerB() throws Exception {
        final Transaction t1 = initiateNewTransaction().transaction();
        defaultScheduler.fire(SipTimer.B);
        mockChannelContext.ensureTransactionTerminated(t1.id());
        assertTimerCancelled(SipTimer.A);
    }

    /**
     * Initiate a new invite client transaction returning both the transaction and the request
     * as it was propagated through the transaction layer.
     *
     * @return
     * @throws Exception
     */
    public SipTransactionEvent initiateNewTransaction() throws Exception {
        final SipRequest invite = defaultInviteRequest.clone();
        // change the branch since we will otherwise actually hit the
        // same transaction and then this will be a re-transmission
        // instead which is not what we want.
        invite.setHeader(CallIdHeader.create());
        final ViaHeader via = invite.getViaHeader().copy().withBranch(ViaHeader.generateBranch()).build();
        invite.setHeader(via);

        final AtomicReference<Transaction> tRef = new AtomicReference<>();
        transactionLayer.createFlow("127.0.0.1")
                .withPort(5070)
                .withTransport(Transport.udp)
                .onSuccess(f -> {
                    final ClientTransaction transaction = transactionLayer.newClientTransaction(f, invite);
                    tRef.set(transaction);
                    transaction.start();
                })
                .onFailure(f -> fail("Creating the flow shouldnt fail"))
                .onCancelled(f -> fail("The flow shouldn't have been cancelled"))
                .connect();


        // the INVITE should have been downstream and therefore should have ended up
        // in our mock ChannelHandlerContext so pick up the invite from there.
        // should have been here;
        // continue from here next time!
        final SipFlowEvent event = mockChannelContext.assertAndConsumeDownstreamRequest("invite");

        assertTimerScheduled(SipTimer.A);
        assertTimerScheduled(SipTimer.B);

        // perhaps this wasn't all that creat since for downstream events the
        // request is turned into a FlowEvent but then again, the TransactionEvent
        // is a simple holder of the two things we need, i.e. the transaction and the
        // sip request.
        return TransactionEvent.create(tRef.get(), event.request());
    }

    /**
     * Initiate a SIP INVITE through the transaction layer where we instruct
     * our "application" to send out an INVITE and then we'll respond back
     * with the specified response.
     *
     * @param finalResponseStatus
     */
    public SipTransactionEvent initiateTransition(final int finalResponseStatus) throws Exception {
        final TransactionEvent holder = initiateNewTransaction();
        final Transaction t1 = holder.transaction();
        final SipRequest request = holder.toSipRequestTransactionEvent().request();

        // create the final response
        final SipResponse response = request.createResponse(finalResponseStatus).build();
        final Flow flow = Mockito.mock(Flow.class);
        transactionLayer.channelRead(mockChannelContext, FlowEvent.create(flow, response));

        // the final response should have been propagated through the transaction layer
        // and then ending up in our "application" (our transaction user mock object).
        final Transaction t2 = mockChannelContext.assertAndConsumeResponse("invite", finalResponseStatus);
        assertThat(t1.id(), is(t2.id()));

        return holder.toSipTransactionEvent();
    }

}