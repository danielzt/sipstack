package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.header.CallIdHeader;
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
public class InviteClientTransactionActorTest extends TransactionTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test(timeout = 1000)
    public void testTransitionToCompleted() throws Exception {
        for (int i = 200; i < 300; ++i) {
            initiateTransition(i);
            assertTimerCancelled(SipTimer.A);
            assertTimerCancelled(SipTimer.B);
            reset();
        }
    }

    @Test(timeout = 500)
    public void testFireTimerA() throws Exception {
        final SipAndTransactionStorage.Holder holder = initiateNewTransaction();
        final Transaction t1 = holder.transaction();
        final SipRequest request = holder.message().toRequest();

        defaultScheduler.fire(SipTimer.A);

        final SipRequest retransmittedRequest = transports.assertRequest("invite");
        assertThat(request, is(retransmittedRequest));
    }

    /**
     * Initiate a new invite client transaction returning both the transaction and the request
     * as it was propagated through the transaction layer.
     *
     * @return
     * @throws Exception
     */
    public SipAndTransactionStorage.Holder initiateNewTransaction() throws Exception {
        final SipRequest invite = defaultInviteRequest.clone();
        // change the branch since we will otherwise actually hit the
        // same transaction and then this will be a re-transmission
        // instead which is not what we want.
        invite.setHeader(CallIdHeader.create());
        invite.getViaHeader().setBranch(ViaHeader.generateBranch());

        // ask our "application", which is just a Transaction User and that is using the
        // transaction layer to send requests/responses, to send an INVITE.
        final Transaction t1 = myApplication.sendRequest(invite);

        // the INVITE should have been sent through the transaction layer down to the
        // transport layer, for which we have a mock implementation so check that we
        // received it there.
        final SipRequest request = transports.assertRequest("invite");
        transports.consumeRequest(request);

        assertTimerScheduled(SipTimer.A);
        assertTimerScheduled(SipTimer.B);

        return new SipAndTransactionStorage.Holder(t1, request);
    }

    /**
     * Initiate a SIP INVITE through the transaction layer where we instruct
     * our "application" to send out an INVITE and then we'll respond back
     * with the specified response.
     *
     * @param finalResponseStatus
     */
    public Transaction initiateTransition(final int finalResponseStatus) throws Exception {
        final SipAndTransactionStorage.Holder holder = initiateNewTransaction();
        final Transaction t1 = holder.transaction();
        final SipRequest request = holder.message().toRequest();

        // create the final response
        final SipResponse response = request.createResponse(finalResponseStatus);
        final Flow flow = Mockito.mock(Flow.class);
        transactionLayer.onMessage(flow, response);

        // the final response should have been propagated through the transaction layer
        // and then ending up in our "application" (our transaction user mock object).
        final Transaction t2 = myApplication.assertAndConsumeResponse("invite", finalResponseStatus);

        // the response as seen by "my application" should of course be the very same
        // transaction as what the original invite request got associated with.
        assertThat(t1.id(), is(t2.id()));

        return t1;
    }

}