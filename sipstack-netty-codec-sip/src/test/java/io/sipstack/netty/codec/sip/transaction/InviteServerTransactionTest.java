/**
 * 
 */
package io.sipstack.netty.codec.sip.transaction;

import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.event.Event;
import org.junit.Before;
import org.junit.Test;

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
        assertNothingWritten();

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
