package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.header.CallIdHeader;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.event.SipRequestTransactionEvent;
import io.sipstack.transport.Flow;
import io.sipstack.transport.event.FlowEvent;
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
        // transaction level so it wouldn't match this ACK to
        // anything anyway...
        transactionLayer.channelRead(mockChannelContext, FlowEvent.create(transaction.flow(), defaultAckRequest));

        final Transaction ackTransaction = mockChannelContext.assertAndConsumeRequest("ack").transaction();
        mockChannelContext.ensureTransactionTerminated(ackTransaction.id());
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

        // clear out any messages so we know that when we issue
        // the re-transmit that it indeed is a re-transmit.
        mockChannelContext.reset();

        defaultScheduler.fire(SipTimer.G);

        mockChannelContext.assertAndConsumeDownstreamResponse("invite", 500);

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
        // remember that the transaction returned here
        // is a immutable snapshot of what the transaction was
        // and cannot therefore be used later on to check if it
        // transitioned to another state.
        final Transaction transaction = transitionToAccepted(200);

        // "fire" timer L
        defaultScheduler.fire(SipTimer.L);

        mockChannelContext.ensureTransactionTerminated(transaction.id());
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
    public Transaction initiateTransition(final int finalResponseStatus) throws Exception {
        // we will be using this header to tell the {@link MockTransactionUser} which response
        // to generate and send back...
        final SipRequest invite = defaultInviteRequest.clone();

        // change the branch since we will otherwise actually hit the
        // same transaction and then this will be a re-transmission
        // instead which is not what we want.
        invite.setHeader(CallIdHeader.create());
        invite.getViaHeader().setBranch(ViaHeader.generateBranch());

        final Flow flow = Mockito.mock(Flow.class);
        transactionLayer.channelRead(mockChannelContext, FlowEvent.create(flow, invite));

        final SipRequestTransactionEvent event = mockChannelContext.assertAndConsumeRequest("invite");

        // so, we have not ensured that the request (the flow event) went through
        // the Transaction Layer and the transaction layer generated a new transaction
        // and passed on the request to the next handler in the pipe. We don't have
        // another handler though but the Transaction Layer doesn't know that :-).
        // In either case, we will invoke the Transport Layer again, now sending
        // a response down the pipe instead, just as if an application would have.
        final SipResponse response = event.request().createResponse(finalResponseStatus);
        event.transaction().send(response);

        // the transaction layer should be invoked again and if the state machine
        // doesn't consume it, which it shouldn't in this case, then the response
        // will be written to the ChannelHandlerContext again, which we then can
        // verify
        mockChannelContext.assertAndConsumeDownstreamResponse("invite", finalResponseStatus);

        return event.transaction();
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