package io.sipstack.transaction.impl;

import io.sipstack.event.SipMsgEvent;
import io.sipstack.transaction.TransactionState;
import org.junit.Test;

/**
 * All tests concerning testing the Proceeding state are found here.
 *
 * @author jonas@jonasborjesson.com
 */
public class NonInviteServerTransactionProceedingStateTest extends InviteServerTransactionTestBase {

    public NonInviteServerTransactionProceedingStateTest() throws Exception {
        // left empty intentionally
    }

    /**
     * Ensure so that we can transition over to the Trying state and
     * relay the request to the next one in the chain.
     *
     * @throws Exception
     */
    @Test
    public void testTransitionToCompleted() throws Exception {
        for (SipMsgEvent msg : nonInviteRequests) {
            for (int i = 200; i < 700; ++i) {
                prepare(msg, i);
            }
        }
    }

    @Test
    public void testRetransmittedRequest() throws Exception {
        for (SipMsgEvent msg : nonInviteRequests) {
            prepare(msg);
            defaultCtx.forward(msg);
            consumeResponses(first, getTransactionId(msg), 100);

            // the re-transmitted request should not hit the next
            // one in the pipe
            assertReceivedRequests(last, 0);
        }
    }

    private void prepare(final SipMsgEvent msg) {
        prepare(msg, -1);
    }

    /**
     * Convenience method for moving over the transaction to the PROCEEDING state
     * so that we can perform the tests.
     *
     * @param msg
     */
    private void prepare(final SipMsgEvent msg, final int responseCode) {
        if (responseCode == -1) {
            init(100);
        } else {
            init(100, responseCode);
        }
        defaultCtx.forward(msg);
        consumeResponses(first, getTransactionId(msg), 100);
        if (responseCode == -1) {
            assertServerTransactionState(msg, TransactionState.PROCEEDING);
        } else {
            assertServerTransactionState(msg, TransactionState.COMPLETED);
            consumeResponses(first, getTransactionId(msg), responseCode);
        }
        consumeRequests(last, getTransactionId(msg), msg.getSipMessage().getMethod().toString());
    }

}