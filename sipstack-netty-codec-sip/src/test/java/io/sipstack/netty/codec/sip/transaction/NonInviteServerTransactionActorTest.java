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
     * When we get a re-transmission then we are supposed to send back
     * the latest response. The re-transmitted request will NOT
     * be passed to the next handler
     *
     * @throws Exception
     */
    // @Test(timeout = 500)
    @Test
    public void testRetransmitOfBye() throws Exception {
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
        assertNothingRead();
    }

    /**
     * Ensure the that the transition from INIT->TRYING->COMPLETED->TERMINATEd is
     * working.
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
        assertNothingWritten();

        // reset the context so all latches etc start
        // over at 1 since it is easier to test that way
        resetChannelHandlerContext(transactionLayer);

        // send in the final response
        final Event responseEvent = createEvent(finalResponse);
        transactionLayer.write(defaultChannelCtx, responseEvent, null);

        // we should now be in the completed state.
        assertTransactionState(defaultByeRequest, TransactionState.COMPLETED);

        // and verify that that final response was actually written to the context
        // and as such also written out the socket (well, eventually, at least
        // it is correctly being handed off to the next handler)
        waitAndAssertMessageWritten(responseEvent);

        // Timer J should have been scheduled.
        assertTimerScheduled(SipTimer.J);
    }
}