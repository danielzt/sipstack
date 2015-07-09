package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.header.CallIdHeader;
import io.pkts.packet.sip.header.SipHeader;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.transaction.Transaction;
import io.sipstack.transport.Flow;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author jonas@jonasborjesson.com
 */
public class InviteServerTransactionActorTest extends TransactionTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * An ACK to a 200 goes in its own transaction, even though it actually doesn't really
     * have a transaction.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testAckTo200() throws Exception {
        final Transaction transaction = transitionToAccepted(200);

        // note, we create the ACK based on the defaultInviteRequest
        // but if that isn't true then we are screwed since it won't
        // be the same dialog. But then again, we are testing the
        // transaction level so it wouldn't matchi this ACK to
        // anything anyway...
        final Flow flow = Mockito.mock(Flow.class);
        transactionLayer.onMessage(flow, defaultAckRequest);

        final Transaction ackTransaction = myApplication.assertAndConsumeRequest("ack");
        assertThat(ackTransaction.flow(), is(flow));

        myApplication.ensureTransactionTerminated(ackTransaction.id());
    }

    /**
     * Ensure that we can transition to the completed state correctly
     *
     * @throws Exception
     */
    @Test(timeout = 2000)
    public void testTransitionToCompleted() throws Exception {
        for (int i = 300; i < 700; ++i) {
            transitionToCompleted(i);
            reset();
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
        transitionToCompleted(500);

        // clear out any messages and reset the countdown latch
        transports.reset();

        defaultScheduler.fire(SipTimer.G);

        transports.latch().await();

        // we should have the 500 being re-transmitted
        transports.assertAndConsumeResponse("invite", 500);

        // and timer G should have been fired again
        assertTimerScheduled(SipTimer.G);
    }

    /**
     * Test the basic transitions of the invite server transaction. I.e.,
     * INVITE followed by a 200 and then the timer L fires which should take
     * us to the TERMINATED state.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testBasicTransition() throws Exception {
        final Transaction transaction = transitionToAccepted(200);

        // "fire" timer L
        defaultScheduler.fire(SipTimer.L);

        myApplication.ensureTransactionTerminated(transaction.id());
    }


    public Transaction transitionToAccepted(final int finalResponseStatus) throws Exception {
        // ensure no one uses this method erroneously. Only 2xx responses!
        assertThat("This convenience method expects a final error response!", finalResponseStatus / 100 == 2, is(true));
        final Transaction transaction = initiateTransition(finalResponseStatus);

        // Timer L should have been scheduled.
        assertTimerScheduled(SipTimer.L);
        return transaction;
    }

    /**
     * Initiate a SIP INVITE through the transaction layer where we instruct
     * our "application" (which implements the Transaction User interface, which is
     * what the transaction layer is emitting its requests/responses to) to
     * generate a response.
     *
     * @param finalResponseStatus
     */
    public Transaction initiateTransition(final int finalResponseStatus) {
        // we will be using this header to tell the {@link MockTransactionUser} which response
        // to generate and send back...
        final SipRequest invite = defaultInviteRequest.clone();
        invite.setHeader(SipHeader.create("X-Transaction-Test-Response", Integer.toString(finalResponseStatus)));

        // change the branch since we will otherwise actually hit the
        // same transaction and then this will be a re-transmission
        // instead which is not what we want.
        invite.setHeader(CallIdHeader.create());
        invite.getViaHeader().setBranch(ViaHeader.generateBranch());

        final Flow flow = Mockito.mock(Flow.class);
        transactionLayer.onMessage(flow, invite);

        // ensure that we indeed received an invite request
        // over in the transaction user (which again is the layer above
        // the Transport Layer and is acting as our application right now)
        final Transaction transaction = myApplication.assertAndConsumeRequest("invite");

        // ensure that the transport layer received the final response
        // as our application, the transaction user, generated.
        // See the {@link MockTransactionUser} for how this is done.
        transports.assertAndConsumeResponse("invite", finalResponseStatus);

        return transaction;
    }

    /**
     * Convenience method for transitioning the invite server transaction over to the
     * COMPLETED state.
     *
     * @param finalResponseStatus
     * @throws Exception
     */
    public void transitionToCompleted(final int finalResponseStatus) throws Exception {
        // ensure no one uses this method erroneously
        assertThat("This convenience method expects a final error response!", finalResponseStatus / 100 > 2, is(true));

        initiateTransition(finalResponseStatus);

        // Timer G and H should have been scheduled.
        assertTimerScheduled(SipTimer.G);
        assertTimerScheduled(SipTimer.H);
    }
}