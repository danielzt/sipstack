package io.sipstack.transaction.impl;

import io.sipstack.config.TimersConfiguration;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.timers.SipTimer;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * All tests concerning the proceeding state is found here.
 *
 * @author jonas@jonasborjesson.com
 */
public class InviteServerTransactionProdeedingStateTest extends InviteServerTransactionTestBase {

    public InviteServerTransactionProdeedingStateTest() throws Exception {
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
     * Test so that an initial invite is handled correctly in that a new transaction is created, a
     * 100 Trying is sent out, the "last" actor receives the invite and the "first" actor indeed
     * receives the 100 Trying.
     *
     */
    @Test
    public void testInitialInvite() {
        defaultCtx.forward(defaultInviteEvent);
        assertInitialInvite(defaultInviteEvent);
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
            final TransactionId id = getTransactionId(defaultInviteEvent);
            peekResponses(first, id, 100, i);
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
     * By default the {@link InviteServerTransactionActor} sends a 100 Trying right away but if
     * configured it will delay it with 200 ms so let's make sure that that's what happens.
     */
    @Test
    public void testSend100TryingAfter200ms() throws Exception {
        final TransactionLayerConfiguration config = new TransactionLayerConfiguration();
        config.setSend100TryingImmediately(false);
        init(config);

        this.defaultCtx.forward(this.defaultInviteEvent);

        // there should be one delayed 100 Trying waiting to be sent.

        assertThat(this.actorSystem.scheduledJobs.size(), is(1));
        assertSheduledTimer(Duration.ofMillis(200), SipTimer.Trying);
        // final DelayedJob scheduledEvent = this.actorSystem.scheduledJobs.get(0);
        // assertThat(scheduledEvent.delay, is(Duration.ofMillis(200)));
        // assertThat(scheduledEvent.job.getEvent().isTimerEvent(), is(true));


        // final Object trying = scheduledEvent.job.getEvent().toTimerEvent().getEvent();

        // our transaction should be in the proceeding state though
        assertServerTransactionState(this.defaultInviteEvent, TransactionState.PROCEEDING);

        // ensure no other responses have been sent.
        assertThat(this.first.responseEvents.isEmpty(), is(true));

        // ok, "execute" the timeout.
        runDelayedJob();

        // and how we should have the 100 trying
        final TransactionId id = getTransactionId(this.defaultInviteEvent);
        peekResponses(this.first, id, 100);
    }

    /**
     * Test a regular invite transaction that succeeds
     */
    @Test
    public void testInviteRinging() {
        init(180);
        this.defaultCtx.forward(this.defaultInviteEvent);

        // should still be in proceeding
        assertServerTransactionState(this.defaultInviteEvent, TransactionState.PROCEEDING);

        // and we have should have received a 180 ringing responses
        // of the initial invite transaction. We should also have received a 100 Trying.
        final TransactionId id = getTransactionId(this.defaultInviteEvent);
        peekResponses(this.first, id, 100, 180);

        // no timers or anything should have been scheduled just yet.
        assertThat(this.actorSystem.scheduledJobs.isEmpty(), is(true));
    }

    @Test
    public void testInviteRinging200Ok() {
        init(180, 200);
        this.defaultCtx.forward(this.defaultInviteEvent);

        // should be in terminated state because of the 200 response
        assertServerTransactionState(this.defaultInviteEvent, TransactionState.ACCEPTED);

        final TransactionId id = getTransactionId(this.defaultInviteEvent);
        peekResponses(this.first, id, 100, 180, 200);
    }

    /**
     * While in the proceeding state, any re-transmitted INVITE requests should result
     * in the last response to be sent out again.
     *
     * @throws Exception
     */
    @Test
    public void testProceedingStateReTransmitInvite() throws Exception {
        init(180);
        defaultCtx.forward(defaultInviteEvent);
        consumeResponses(this.first, defaultInviteTransactionId, 100, 180);

        // the last handler should have received the first invite.
        consumeRequests(last, defaultInviteTransactionId, "INVITE");

        // re-transmit the invite and we should be getting the 180 back
        // again AND the "TU" should NOT see this re-transmission.
        defaultCtx.forward(defaultInviteEvent);
        consumeResponses(first, defaultInviteTransactionId, 180);
        assertReceivedRequests(last, 0);
    }
}