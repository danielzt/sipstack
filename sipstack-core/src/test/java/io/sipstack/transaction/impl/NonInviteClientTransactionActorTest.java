package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.header.CallIdHeader;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionState;
import io.sipstack.transaction.impl.SipAndTransactionStorage.Holder;
import io.sipstack.transport.Flow;
import org.junit.Before;
import org.junit.Test;

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
        final Holder holder = initiateNewTransaction("bye");
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
        myApplication.ensureTransactionTerminated(transaction.id());
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
        final Holder holder = transitionFromTryingToCompleted("bye", 200);
        defaultScheduler.fire(SipTimer.K);
        myApplication.ensureTransactionTerminated(holder.transaction().id());
    }

    /**
     * Ensure we can go the path Trying -> Proceeding -> Completed
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testTransitionToCompletedFromProceeding() throws Exception {
        // TODO: test all combinations of provisional and all known SIP methods
        final Holder holder = transitionFromTryingToProceeding("bye", 100);
    }

    /**
     * Test the full transition between:
     * Trying -> Proceeding -> Completed -> Terminated
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testTransitionTryingProceedingCompletedTerminated() throws Exception {
        final Holder holder = transitionFromTryingToProceeding("bye", 100);
        final SipRequest request = holder.message().toRequest();
        final SipResponse response = request.createResponse(200);
        transactionLayer.onMessage(mock(Flow.class), response);
        myApplication.assertAndConsumeResponse("bye", 200);

        // should now be in completed so timer K should have been
        // scheduled
        assertTimerScheduled(SipTimer.K);

        // fire timer K which then takes us to terminated
        defaultScheduler.fire(SipTimer.K);
        myApplication.ensureTransactionTerminated(holder.transaction().id());
    }

    /**
     * While in proceeding, any provisional responses should be passed to the TU
     * and that's it...
     */
    @Test(timeout = 500)
    public void test1xxResponsesWhileInProceedingState() throws Exception {
        final Holder holder = transitionFromTryingToProceeding("bye", 100);
        final SipRequest request = holder.message().toRequest();
        final SipResponse response = request.createResponse(180);
        transactionLayer.onMessage(mock(Flow.class), response);
        myApplication.assertAndConsumeResponse("bye", 180);
    }

    /**
     * Timer E will always control re-transmission so ensure this is true in
     * the proceeding state as well.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testTimerEWhileInProceedingState() throws Exception {
        final Holder holder = transitionFromTryingToProceeding("bye", 100);
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
        final Holder holder = transitionFromTryingToProceeding("bye", 100);
        defaultScheduler.fire(SipTimer.F);
        assertTimerCancelled(SipTimer.E);
        myApplication.ensureTransactionTerminated(holder.transaction().id());
    }



    /**
     * Helper method to transition Trying -> Completed.
     *
     * @param responseCode
     * @return
     * @throws Exception
     */
    private Holder transitionFromTryingToCompleted(final String method, final int responseStatus) throws Exception {
        // ensure no one is using this method wrong
        assertThat("Only final responses pls!", responseStatus >= 200, is(true));

        final Holder holder = initiateNewTransaction(method);
        final SipRequest request = holder.message().toRequest();
        final SipResponse response = request.createResponse(responseStatus);
        transactionLayer.onMessage(mock(Flow.class), response);
        myApplication.assertAndConsumeResponse(method, responseStatus);

        assertTimerScheduled(SipTimer.K);
        return holder;
    }

    /**
     * Helper method to transition Trying -> Proceeding
     */
    private Holder transitionFromTryingToProceeding(final String method, final int responseStatus) throws Exception {
        // ensure no one is using this method wrong
        assertThat("Only provisional responses pls!", responseStatus / 100 == 1, is(true));

        final Holder holder = initiateNewTransaction(method);
        final SipRequest request = holder.message().toRequest();
        final SipResponse response = request.createResponse(responseStatus);
        transactionLayer.onMessage(mock(Flow.class), response);
        myApplication.assertAndConsumeResponse(method, responseStatus);

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
    public Holder initiateNewTransaction(final String method) throws Exception {
        final SipRequest request = generateRequest(method);

        // change the branch since we will otherwise actually hit the
        // same transaction and then this will be a re-transmission
        // instead which is not what we want.
        request.setHeader(CallIdHeader.create());
        request.getViaHeader().setBranch(ViaHeader.generateBranch());

        // ask our "application", which is just a Transaction User and that is using the
        // transaction layer to send requests/responses, to send an INVITE.
        myApplication.sendRequest(request);

        // We need to know what transaction the "transaction user" (i.e. our app) got
        // for the request it just sent. Our application is storing
        // all transaction so we can look it up.
        final Transaction t1 = myApplication.assertTransaction(request);
        assertThat(t1.state(), is(TransactionState.TRYING));

        // the request should have been sent through the transaction layer down to the
        // transport layer, for which we have a mock implementation so check that we
        // received it there.
        final SipRequest request2 = transports.assertRequest(method);
        transports.consumeRequest(request2);

        assertTimerScheduled(SipTimer.E);
        assertTimerScheduled(SipTimer.F);

        return new Holder(t1, request);
    }

    private SipRequest generateRequest(final String method) {
        if ("BYE".equalsIgnoreCase(method)) {
            return defaultByeRequest;
        }

        fail("I am not handling that method right now. Add above...");
        return null;
    }

}