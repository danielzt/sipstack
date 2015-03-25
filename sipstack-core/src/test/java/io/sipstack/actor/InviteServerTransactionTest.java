/**
 * 
 */
package io.sipstack.actor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.event.Event;
import io.sipstack.event.SipEvent;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;
import io.sipstack.transaction.impl.InviteServerTransactionActor;
import io.sipstack.transaction.impl.TransactionSupervisor;

import java.time.Duration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public class InviteServerTransactionTest extends SipTestBase {

    /**
     * The transaction supervisor used for all tests within this test class.
     */
    private TransactionSupervisor supervisor;

    /**
     * The fake timer used for checking whether tasks where scheduled for later execution.
     */
    private MockTimer timer;

    /**
     * An event proxy that is inserted before the supervisor.
     */
    private EventProxy first;

    /**
     * An event proxy that is inserted after the supervisor.
     */
    private EventProxy last;

    /**
     * All tests herein uses the same pipeline setup. I.e. first -> supervisor -> last.
     * 
     * By using this setup, we can drive all the events we need to test the transaction
     * state machine.
     */
    private PipeLineFactory factory;

    /**
     * Most scenarios start off with an invite and if so, just use this one.
     */
    private SipEvent defaultInviteEvent;

    /**
     * Default 180 Ringing that is in the same transaction as the {@link #defaultInviteEvent}.
     */
    private SipEvent default180RingingEvent;

    /**
     * Default 200 OK that is in the same transaction as the {@link #defaultInviteEvent}.
     */
    private SipEvent default200OKEvent;

    /**
     * Since everything is pretty much set in this test class the {@link ActorContext} is always
     * starting off the same way as well, which is really just reflecting how the {@link PipeLine}
     * is configured.
     */
    private ActorContext defaultCtx;

    private MockActorSystem actorSystem;

    public InviteServerTransactionTest() throws Exception {
        super();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        init();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Setup the system with the option to respond with a set of responses. The responses will be
     * created by the {@link EventProxy} that is configured last in the pipeline and is as such
     * acting as the application using the transaction.
     * 
     * @param responses
     */
    protected void init(final Integer... responses) {
        init(this.sipConfig.getTransaction(), responses);
    }

    protected void init(final TransactionLayerConfiguration config, final Integer... responses) {

        // final ActorSystem.Builder builder = ActorSystem.withName("unit tests");
        // builder.withConfiguration(this.sipConfig);
        // builder.withTimer(this.timer);

        this.timer = new MockTimer();

        this.first = new EventProxy();
        this.last = new EventProxy(responses);
        this.supervisor = new TransactionSupervisor(config);

        this.factory = PipeLineFactory.withDefaultChain(this.first, this.supervisor, this.last);
        this.actorSystem = new MockActorSystem(this.factory);

        this.defaultInviteEvent = SipEvent.create(this.invite);
        this.default180RingingEvent = SipEvent.create(this.ringing);
        this.default200OKEvent = SipEvent.create(this.twoHundredToInvite);

        this.defaultCtx = ActorContext.withPipeLine(this.actorSystem, this.factory.newPipeLine());

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
            this.defaultCtx.forward(this.defaultInviteEvent);
            final TransactionId id = getTransactionId(this.defaultInviteEvent);
            assertResponses(this.first, id, 100, i);
            assertServerTransactionState(this.defaultInviteEvent, TransactionState.COMPLETED);

            // We should also have setup a timer G for re-transmitting the response
            // and that delay should the first time around be the same as T1.
            // Hence make sure that there is a response ready to be sent out and the
            // response code is the same as what we just created (i.e. equal to loop variable 'i')
            // and the delay is T1
            final Duration t1 = this.sipConfig.getTransaction().getTimers().getT1();
            assertThat(this.actorSystem.scheduledJobs.size(), is(1));
            final DelayedJob scheduledEvent = this.actorSystem.scheduledJobs.get(0);
            assertThat(scheduledEvent.delay, is(t1));
            assertThat(scheduledEvent.job.getDirection(), is(Direction.DOWNSTREAM));
            assertThat(scheduledEvent.job.getEvent().isSipEvent(), is(true));
            final SipEvent trying = (SipEvent) scheduledEvent.job.getEvent();
            assertThat(trying.getSipMessage().isResponse(), is(true));
            assertThat(trying.getSipMessage().toResponse().getStatus(), is(i));
        }
    }


    /**
     * Test so that an initial invite is handled correctly in that a new transaction is created, a
     * 100 Trying is sent out, the "last" actor receives the invite and the "first" actor indeed
     * receives the 100 Trying.
     * 
     */
    @Test
    public void testInitialInvite() {
        this.defaultCtx.forward(this.defaultInviteEvent);
        assertInitialInvite(this.defaultInviteEvent);
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
        final DelayedJob scheduledEvent = this.actorSystem.scheduledJobs.get(0);
        assertThat(scheduledEvent.delay, is(Duration.ofMillis(200)));
        assertThat(scheduledEvent.job.getDirection(), is(Direction.DOWNSTREAM));
        assertThat(scheduledEvent.job.getEvent().isTimerEvent(), is(true));
        final SipEvent trying = (SipEvent) scheduledEvent.job.getEvent().toTimerEvent().getEvent();
        assertThat(trying.getSipMessage().isResponse(), is(true));
        assertThat(trying.getSipMessage().toResponse().is100Trying(), is(true));

        // our transaction should be in the proceeding state though
        assertServerTransactionState(this.defaultInviteEvent, TransactionState.PROCEEDING);

        // ensure no other responses have been sent.
        assertThat(this.first.downstreamEvents.isEmpty(), is(true));

        // ok, "execute" the timeout.
        scheduledEvent.job.run();

        // and how we should have the 100 trying
        final TransactionId id = getTransactionId(this.defaultInviteEvent);
        assertResponses(this.first, id, 100);
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
        assertResponses(this.first, id, 100, 180);

        // no timers or anything should have been scheduled just yet.
        assertThat(this.actorSystem.scheduledJobs.isEmpty(), is(true));
    }

    @Test
    public void testInviteRinging200Ok() {
        init(180, 200);
        this.defaultCtx.forward(this.defaultInviteEvent);

        // should be in terminated state because of the 200 response
        assertServerTransactionState(this.defaultInviteEvent, TransactionState.TERMINATED);

        final TransactionId id = getTransactionId(this.defaultInviteEvent);
        assertResponses(this.first, id, 100, 180, 200);
    }

    /**
     * Helper method for making sure that the "first" Actor has received the responses for a
     * particular transaction.
     * 
     * @param id
     * @param expectedResponses
     */
    private void assertResponses(final EventProxy proxy, final TransactionId id, final Integer...expectedResponses) {
        for (int i = 0; i < expectedResponses.length; ++i) {
            final SipEvent event = (SipEvent)proxy.downstreamEvents.get(i);
            final SipMessage msg = event.getSipMessage();
            assertThat(msg.isResponse(), is(true));
            assertThat(getTransactionId(event), is(id));
            assertThat(msg.toResponse().getStatus(), is(expectedResponses[i]));
        }
    }

    /**
     * Ensure that the initial invite was processed correctly. Remember that because of how the pipe
     * lines work, everything is executed on the current thread so it is very easy to test. Also,
     * all tests within this test class is setup the same way since we are testing the transaction
     * only, making verifying the tests very easy.
     * 
     * @param initialInvite
     */
    private void assertInitialInvite(final SipEvent initialInvite) {
        // the transaction should be in the proceeding state
        assertServerTransactionState(initialInvite, TransactionState.PROCEEDING);

        // the "last" should have received the invite event that was pushed
        // through and nothing else.
        assertThat(this.last.upstreamEvents.size(), is(1));
        assertThat(this.last.upstreamEvents.get(0), is(initialInvite));
        assertThat(this.last.downstreamEvents.size(), is(0));

        // the "first" should have received the invite event as well
        // as a 100 Trying response...
        assertThat(this.first.upstreamEvents.size(), is(1));
        assertThat(this.first.upstreamEvents.get(0), is(initialInvite));

        assertThat(this.first.downstreamEvents.size(), is(1));
        assertThat(getSipMessage(this.first.downstreamEvents.get(0)).isResponse(), is(true));
        assertThat(getSipMessage(this.first.downstreamEvents.get(0)).toResponse().is100Trying(), is(true));

        this.first.reset();
        this.last.reset();
    }

    private SipMessage getSipMessage(final Event event) {
        return ((SipEvent) event).getSipMessage();
    }

    /**
     * Helper method for asserting the transaction state for the transaction identified by the sip
     * event.
     * 
     * @param event
     * @param state
     */
    private void assertServerTransactionState(final SipEvent event, final TransactionState state) {
        final Transaction transaction = this.supervisor.getTransaction(TransactionId.create(event.getSipMessage()));

        // Note that when the server transaction enters terminated state it will
        // be purged from memory right away...
        if (state == TransactionState.TERMINATED) {
            assertThat(transaction, is((Transaction) null));
        } else {
            assertThat(transaction.isServerTransaction(), is(true));
            assertThat(transaction.isClientTransaction(), is(false));
            assertThat(transaction.getState(), is(state));
        }
    }

    private TransactionId getTransactionId(final SipEvent event) {
        return TransactionId.create(event.getSipMessage());
    }
}
