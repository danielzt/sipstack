package io.sipstack.transaction.impl;

import io.sipstack.event.SipMsgEvent;
import io.sipstack.transaction.TransactionState;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * All tests concerning testing the Trying state are found here.
 *
 * @author jonas@jonasborjesson.com
 */
public class NonInviteServerTransactionTryingStateTest extends InviteServerTransactionTestBase {

    /**
     * The list of all non-invite requests. Typically, ALL tests that we execute will
     * go through all requests there are in SIPs various specifications.
     */
    private List<SipMsgEvent> nonInviteRequests;

    public NonInviteServerTransactionTryingStateTest() throws Exception {
        // left empty intentionally
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        nonInviteRequests = Arrays.asList(defaultByeEvent);
    }

    /**
     * Ensure so that we can transition over to the Trying state and
     * relay the request to the next one in the chain.
     *
     * @throws Exception
     */
    @Test
    public void testTransitionToTrying() throws Exception {
        for (SipMsgEvent msg : nonInviteRequests) {
            init();
            defaultCtx.forward(msg);
            assertServerTransactionState(msg, TransactionState.TRYING);

            // the last handler should have received the BYE
            consumeRequests(last, getTransactionId(msg), msg.getSipMessage().getMethod().toString());
        }
    }

    /**
     * Make sure that we correctly transition over to the completed state
     * for any final response.
     *
     * @throws Exception
     */
    @Test
    public void testAllFinalResponses() throws Exception {
        for (SipMsgEvent msg : nonInviteRequests) {
            for (int i = 200; i < 700; ++i) {
                init(i);
                defaultCtx.forward(msg);
                consumeResponses(first, getTransactionId(msg), i);
                assertServerTransactionState(msg, TransactionState.COMPLETED);
            }
        }
    }

    /**
     * Make sure that we correctly transition over to the proceeding state
     * for any provisional responses.
     *
     * @throws Exception
     */
    @Test
    public void testAllProvisionalResponses() throws Exception {
        for (SipMsgEvent msg : nonInviteRequests) {
            for (int i = 100; i < 200; ++i) {
                init(i);
                defaultCtx.forward(msg);
                consumeResponses(first, getTransactionId(msg), i);
                assertServerTransactionState(msg, TransactionState.PROCEEDING);
            }
        }
    }


}