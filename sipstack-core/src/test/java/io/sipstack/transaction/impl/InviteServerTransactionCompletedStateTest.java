package io.sipstack.transaction.impl;

import io.sipstack.config.TimersConfiguration;
import io.sipstack.timers.SipTimer;
import io.sipstack.transaction.TransactionState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

/**
 * All tests concerning testing the Completed state are found here.
 *
 * @author jonas@jonasborjesson.com
 */
public class InviteServerTransactionCompletedStateTest extends InviteServerTransactionTestBase {

    public InviteServerTransactionCompletedStateTest() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();;
    }

    /**
     * Make sure that if we get an error responses to the INVITE that the transaction is
     * transitioning to the correct state and that we schedule the appropriate timers.
     *
     * @throws Exception
     */
    @Test
    public void testAllErrorResponses() throws Exception {
        for (int i = 300; i < 700; ++i) {
            init(i);
            defaultCtx.forward(defaultInviteEvent);
            peekResponses(first, defaultInviteTransactionId, 100, i);
            assertServerTransactionState(defaultInviteEvent, TransactionState.COMPLETED);

            // We should also have setup a timer G for re-transmitting the response
            // and that delay should the first time around be the same as T1.
            // Hence make sure that there is a response ready to be sent out and the
            // response code is the same as what we just created (i.e. equal to loop variable 'i')
            // and the delay is T1
            TimersConfiguration conf = sipConfig.getTransaction().getTimers();
            assertSheduledTimer(conf.getT1(), SipTimer.G);

            assertSheduledTimer(conf.getTimerH(), SipTimer.H);
        }
    }

    /**
     * While in the completed state, any re-transmitted INVITEs should be absorbed (not passed onto the TU)
     * and the last response should be re-transmitted.
     *
     * @throws Exception
     */
    @Test
    public void testRetransmittedInvite() throws Exception {
        init(603);
        defaultCtx.forward(defaultInviteEvent);
        consumeResponses(first, defaultInviteTransactionId, 100, 603);
        assertServerTransactionState(defaultInviteEvent, TransactionState.COMPLETED);

        // the last handler should have received the first invite.
        consumeRequests(last, defaultInviteTransactionId, "INVITE");

        // re-transmit the invite and we should be getting the 600 back
        // again AND the "TU" should NOT see this re-transmission.
        defaultCtx.forward(defaultInviteEvent);
        consumeResponses(first, defaultInviteTransactionId, 603);
        assertReceivedRequests(last, 0);
    }
    /**
     * Whenever a invite server transaction ends up in the completed state we will schedule time G
     * for re-tranmission of the error response if we haven't received the ACK.
     *
     * @throws Exception
     */
    @Test
    public void testTimerG() throws Exception {
        init(500);
        defaultCtx.forward(this.defaultInviteEvent);

        // should be a 100 and a 500 sent.
        consumeResponses(first, defaultInviteTransactionId, 100, 500);

        // and one scheduled event for Timer G
        assertSheduledTimer(Duration.ofMillis(500), SipTimer.G);

        // run that job
        fireTimer(SipTimer.G);

        // and the 500 should have been re-transmitted.
        consumeResponses(this.first, defaultInviteTransactionId, 500);
    }

    /**
     * When in completed state we will schedule Timer H that will take us
     * to the terminated state when it fires.
     *
     * @throws Exception
     */
    @Test
    public void testTimerH() throws Exception {
        init(500);
        defaultCtx.forward(this.defaultInviteEvent);

        assertSheduledTimer(Duration.ofSeconds(32), SipTimer.H);

        fireTimer(SipTimer.H);

        assertServerTransactionState(defaultInviteEvent, TransactionState.TERMINATED);
    }

}