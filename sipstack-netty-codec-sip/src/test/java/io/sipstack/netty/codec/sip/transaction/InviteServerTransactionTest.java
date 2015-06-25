/**
 * 
 */
package io.sipstack.netty.codec.sip.transaction;

import io.pkts.packet.sip.SipResponse;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.event.Event;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author jonas@jonasborjesson.com
 * 
 */
public class InviteServerTransactionTest extends TransactionTestBase {

    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Ensure that we can transition to the completed state correctly
     *
     * @throws Exception
     */
    @Test(timeout = 1000)
    public void testTransitionToCompleted() throws Exception {
        for (int i = 300; i < 700; ++i) {
            transitionToCompleted(defaultInviteRequest.createResponse(i));
        }
    }

    /**
     * Timer G is for re-transmitting the error response until we see the ACK
     * so fire it off and ensure that the last response actually gets
     * retransmitted.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testTimerG() throws Exception {
        final SipResponse response = defaultInviteRequest.createResponse(500);
        transitionToCompleted(response);

        resetChannelHandlerContext(transactionLayer);
        defaultScheduler.fire(SipTimer.G);

        Thread.sleep(100);
        waitAndAssertMessageWritten(response);
    }

    public void transitionToCompleted(final SipResponse finalResponse) throws Exception {
        // ensure no one uses this method erroneously
        assertThat("This convenience method expects a final response!", finalResponse.isFinal(), is(true));
        assertThat("This convenience method expects a final error response!", finalResponse.isSuccess(), is(false));

        // ensure we start from scratch...
        resetTransactionLayer();
        resetChannelHandlerContext(transactionLayer);

        final Event invite = createEvent(defaultInviteRequest);
        transactionLayer.channelRead(defaultChannelCtx, invite);

        // the invite transaction should have been forwarded to the
        // next handler in the pipe
        waitAndAssertMessageForwarded(invite);
        assertTransactionState(defaultInviteRequest, TransactionState.PROCEEDING);

        // and no downstream events at all...
        defaultChannelCtx.assertNothingWritten();

        // reset the context so all latches etc start
        // over at 1 since it is easier to test that way
        resetChannelHandlerContext(transactionLayer);

        // send a the final error response
        final Event responseEvent = createEvent(finalResponse);
        transactionLayer.write(defaultChannelCtx, responseEvent, null);

        assertTransactionState(defaultInviteRequest, TransactionState.COMPLETED);

        // and verify that that final response was actually written to the context
        // and as such also written out the socket (well, eventually, at least
        // it is correctly being handed off to the next handler)
        waitAndAssertMessageWritten(responseEvent);

        // Timer G and H should have been scheduled.
        assertTimerScheduled(SipTimer.G);
        assertTimerScheduled(SipTimer.H);
    }

    /**
     * Test the basic transitions of the invite server transaction. I.e.,
     * INVITE followed by a 200.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testBasicTransition() throws Exception {
        resetChannelHandlerContext(transactionLayer);
        final Event invite = createEvent(defaultInviteRequest);
        transactionLayer.channelRead(defaultChannelCtx, invite);

        // the invite transaction should have been forwarded to the
        // next handler in the pipe
        waitAndAssertMessageForwarded(invite);
        assertTransactionState(defaultInviteRequest, TransactionState.PROCEEDING);

        // and no downstream events at all...
        defaultChannelCtx.assertNothingWritten();

        // reset the context so all latches etc start
        // over at 1 since it is easier to test that way
        resetChannelHandlerContext(transactionLayer);

        // send a 200 OK...
        final Event twoHundred = createEvent(defaultInvite200Response);
        transactionLayer.write(defaultChannelCtx, twoHundred, null);

        assertTransactionState(defaultInviteRequest, TransactionState.ACCEPTED);

        // and verify that that 200 OK was actually written to the context
        // and as such also written out the socket (well, eventually, at least
        // it is correctly being handed off to the next handler)
        waitAndAssertMessageWritten(twoHundred);

        // Timer L should have been scheduled.
        assertTimerScheduled(SipTimer.L);

        // "fire" timer L
        defaultScheduler.fire(SipTimer.L);
        assertNoTransaction(defaultInviteRequest);
    }
}
