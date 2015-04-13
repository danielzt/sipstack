package io.sipstack.transaction.impl;

import io.sipstack.event.SipMsgEvent;
import io.sipstack.timers.SipTimer;
import io.sipstack.transaction.TransactionState;
import org.junit.Test;

import java.time.Duration;

/**
 * All tests concerning testing the COMPLETED state are found here.
 *
 * @author jonas@jonasborjesson.com
 */
public class NonInviteServerTransactionCompletedStateTest extends InviteServerTransactionTestBase {

    public NonInviteServerTransactionCompletedStateTest() throws Exception {
        // left empty intentionally
    }

    /**
     * Ensure so that we can transition over to the Completed state and
     * that Timer J is correctly scheduled.
     *
     * @throws Exception
     */
    @Test
    public void testTransitionToCompleted() throws Exception {
        for (SipMsgEvent msg : nonInviteRequests) {
            for (int i = 200; i < 700; ++i) {
                prepare(msg, i);

                // For UDP, Timer J should have been scheduled and default
                // duration is 32 seconds.
                assertSheduledTimer(Duration.ofSeconds(32), SipTimer.J);
            }
        }
    }

    /**
     * Make sure that we actually honor the default value of Timer J by changing
     * T1.
     *
     * @throws Exception
     */
    @Test
    public void testTimerJ() throws Exception {
        sipConfig.getTransaction().getTimers().setT1(Duration.ofSeconds(1));
        for (SipMsgEvent msg : nonInviteRequests) {
            prepare(msg, 200);
            // Since Timer J is 32 * T1 we should now be looking at
            // 64 seconds
            assertSheduledTimer(Duration.ofSeconds(64), SipTimer.J);
        }
    }


    /**
     * Convenience method for moving over the transaction to the PROCEEDING state
     * so that we can perform the tests.
     *
     * @param msg
     */
    private void prepare(final SipMsgEvent msg, final int responseCode) {
        init(responseCode);
        defaultCtx.forward(msg);
        consumeResponses(first, getTransactionId(msg), responseCode);
        assertServerTransactionState(msg, TransactionState.COMPLETED);
        consumeRequests(last, getTransactionId(msg), msg.getSipMessage().getMethod().toString());
    }

}