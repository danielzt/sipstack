package io.sipstack.netty.codec.sip.transaction;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.event.Event;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author jonas@jonasborjesson.com
 */
public class NonInviteServerTransactionActorTest extends TransactionTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test the normal transaction transitions with all possible combinations of
     * non-invite requests and final responses.
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testBasicTransactionTransition() throws Exception {
        testTransitionTryingCompletedTerminated(defaultByeRequest, defaultBye200Response);
    }

    /**
     * Ensure that we can transition to the proceeding state correctly. Testing
     * the following path
     *
     * <pre>
     *                    |
     *                    |1xx from TU
     *                    |send response
     *                    |
     * Request            V      1xx from TU
     * send response+-----------+send response
     *     +--------|           |--------+
     *     |        | Proceeding|        |
     *     +------->|           |<-------+
     *              |           |
     *              +-----------+
     *
     * </pre>
     *
     * @throws Exception
     */
    @Test(timeout = 1000)
    public void testTransitionToProceeding() throws Exception {
        for (int i = 100; i < 200; ++i) {
            // must reset the transaction layer since we send in the same
            // bye all the time and as such, we will hit the same transaction
            // and this test is not about multiple responses from TU
            resetTransactionLayer();
            testTransitionToProceeding(defaultByeRequest, defaultByeRequest.createResponse(i));
        }
    }

    /**
     * Ensure that we can transition to the proceeding state correctly
     * and once there we deal with more responses from the TU accordingly
     *
     * <pre>
     *       |
     *       |1xx from TU
     *       |send response
     *       |
     *       V      1xx from TU
     * +-----------+send response
     * |           |--------+
     * | Proceeding|        |
     * |           |<-------+
     * |           |
     * +-----------+
     *
     * </pre>
     *
     * @throws Exception
     */
    @Test(timeout = 1000)
    public void testProceedingMoreResponsesFromTU() throws Exception {
        testTransitionToProceeding(defaultByeRequest, defaultByeRequest.createResponse(100));
    }

    /**
     * Tests that once we are in the proceeding state and receive retransmitted
     * requests we deal with it accordingly, which is to re-transmit the
     * previous response.
     *
     * <pre>
     *                    |
     *                    |1xx from TU
     *                    |send response
     *                    |
     * Request            V
     * send response+-----------+
     *     +--------|           |
     *     |        | Proceeding|
     *     +------->|           |
     *              |           |
     *              +-----------+
     *
     * </pre>
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testProceedingRetransmittedRequest() throws Exception {
        testTransitionToProceeding(defaultByeRequest, defaultByeRequest.createResponse(100));

        // retransmit the bye again...
        resetChannelHandlerContext();
        final Event requestEvent = createEvent(defaultByeRequest);
        transactionLayer.channelRead(defaultChannelCtx, requestEvent);

        // which must not be propagated to the TU
        defaultChannelCtx.assertNothingRead();

        // but the 100 trying should have been retransmitted
        waitAndAssertMessageWritten(defaultByeRequest.createResponse(100));
    }

    /**
     * When we get a re-transmission then we are supposed to send back
     * the latest response. The re-transmitted request will NOT
     * be passed to the next handler. Hence, tests the following
     * part of the state machine...
     *
     * <pre>
     *                      |
     *                      |200-699 from TU
     *                      |send response
     *   Request            V
     *   send response+-----------+
     *       +--------|           |
     *       |        | Completed |
     *       +------->|           |
     *                |           |
     *                +-----------+
     *
     * </pre>
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testCompletedRetransmittedRequest() throws Exception {
        // first get us over to the completed state.
        testTransitionToCompleted(defaultByeRequest, defaultBye200Response);

        resetChannelHandlerContext(transactionLayer);
        // then send in a re-transmission...
        final Event requestEvent = createEvent(defaultByeRequest);
        transactionLayer.channelRead(defaultChannelCtx, requestEvent);

        // ensure that the response was actually
        // re-transmitted again.
        waitAndAssertMessageWritten(defaultBye200Response);

        // the re-transmission should NOT have been passed up
        // to the next handler in the chain.
        defaultChannelCtx.assertNothingRead();
    }

    /**
     * Ensure the that the transition from INIT->TRYING->COMPLETED->TERMINATED is
     * working.
     *
     * Hence, tests the following path (the happy path)
     *
     * <pre>
     *
     *          |Request received
     *          |pass to TU
     *          V
     *    +-----------+
     *    |           |
     *    | Trying    |-------------+
     *    |           |             |
     *    +-----------+             |200-699 from TU
     *                              |send response
     *                              |
     *                              |
     *                              |
     *                              |
     *    +-----------+             |
     *    |           |             |
     *    | Proceeding|             |
     *    |           |             |
     *    |           |             |
     *    +-----------+             |
     *                              |
     *                              |
     *                              |
     *                              |
     *                              |
     *    +-----------+             |
     *    |           |             |
     *    | Completed |<------------+
     *    |           |
     *    |           |
     *    +-----------+
     *          |
     *          |Timer J fires
     *          |-
     *          |
     *          V
     *    +-----------+
     *    |           |
     *    | Terminated|
     *    |           |
     *    +-----------+
     *
     * </pre>
     *
     * @param request
     * @param finalResponse
     * @throws Exception
     */
    private void testTransitionTryingCompletedTerminated(final SipRequest request, final SipResponse finalResponse) throws Exception {
        testTransitionToCompleted(request, finalResponse);

        // "fire" timer J
        defaultScheduler.fire(SipTimer.J);
        assertNoTransaction(defaultByeRequest);
    }

    /**
     * Ensure that the transition INIT->Trying->COMPLETED is working
     * as expected.
     *
     * @param request the initial request.
     * @param finalResponse the final response to the above request
     * @throws Exception
     */
    private void testTransitionToCompleted(final SipRequest request, final SipResponse finalResponse) throws Exception {
        // just ensure no misuse of this method.
        assertThat("This convenience method expects a final response!", finalResponse.isFinal(), is(true));
        assertThat("The response has to be a response to the request!", request.getMethod(), is(finalResponse.getMethod()));

        resetChannelHandlerContext(transactionLayer);
        final Event requestEvent = createEvent(request);
        transactionLayer.channelRead(defaultChannelCtx, requestEvent);

        // the non-invite transaction should have been forwarded to the
        // next handler in the pipe and the state should be trying
        waitAndAssertMessageForwarded(requestEvent);
        assertTransactionState(request, TransactionState.TRYING);

        // and no downstream events at all...
        defaultChannelCtx.assertNothingWritten();

        // reset the context so all latches etc start
        // over at 1 since it is easier to test that way
        resetChannelHandlerContext(transactionLayer);

        // send in the final response
        final Event responseEvent = createEvent(finalResponse);
        transactionLayer.write(defaultChannelCtx, responseEvent, null);

        // we should now be in the completed state.
        assertTransactionState(request, TransactionState.COMPLETED);

        // and verify that that final response was actually written to the context
        // and as such also written out the socket (well, eventually, at least
        // it is correctly being handed off to the next handler)
        waitAndAssertMessageWritten(responseEvent);

        // Timer J should have been scheduled.
        assertTimerScheduled(SipTimer.J);
    }

    /**
     * Ensure that the transition INIT->Trying->Proceeding is working
     * as expected.
     *
     * @param request the initial request.
     * @param provisionalResponse the 1xx response to the above request
     * @throws Exception
     */
    private void testTransitionToProceeding(final SipRequest request, final SipResponse provisionalResponse) throws Exception {
        // just ensure no misuse of this method.
        assertThat("This convenience method expects a final response!", provisionalResponse.isProvisional(), is(true));
        assertThat("The response has to be a response to the request!", request.getMethod(), is(provisionalResponse.getMethod()));

        resetChannelHandlerContext(transactionLayer);
        final Event requestEvent = createEvent(request);
        transactionLayer.channelRead(defaultChannelCtx, requestEvent);

        // the non-invite transaction should have been forwarded to the
        // next handler in the pipe and the state should be trying
        waitAndAssertMessageForwarded(requestEvent);
        assertTransactionState(request, TransactionState.TRYING);

        // and no downstream events at all...
        defaultChannelCtx.assertNothingWritten();

        // reset the context so all latches etc start
        // over at 1 since it is easier to test that way
        resetChannelHandlerContext(transactionLayer);

        // send in the final response
        final Event responseEvent = createEvent(provisionalResponse);
        transactionLayer.write(defaultChannelCtx, responseEvent, null);

        // we should now be in the completed state.
        assertTransactionState(request, TransactionState.PROCEEDING);

        // and verify that that provisional response was actually written to the context
        // and as such also written out the socket (well, eventually, at least
        // it is correctly being handed off to the next handler)
        waitAndAssertMessageWritten(responseEvent);
    }
}