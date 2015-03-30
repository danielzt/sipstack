/**
 *
 */
package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.PipeLine;
import io.sipstack.actor.PipeLineFactory;
import io.sipstack.actor.SipTestBase;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.event.Event;
import io.sipstack.event.SipMsgEvent;
import io.sipstack.timers.SipTimer;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;
import org.junit.After;
import org.junit.Before;

import java.time.Duration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author jonas@jonasborjesson.com
 */
public class InviteServerTransactionTestBase extends SipTestBase {

    /**
     * The transaction supervisor used for all tests within this test class.
     */
    protected TransactionSupervisor supervisor;

    /**
     * The fake timer used for checking whether tasks where scheduled for later execution.
     */
    protected MockTimer timer;

    /**
     * An event proxy that is inserted before the supervisor.
     */
    protected EventProxy first;

    /**
     * An event proxy that is inserted after the supervisor.
     */
    protected EventProxy last;

    /**
     * All tests herein uses the same pipeline setup. I.e. first -> supervisor -> last.
     * <p/>
     * By using this setup, we can drive all the events we need to test the transaction
     * state machine.
     */
    protected PipeLineFactory factory;

    /**
     * Most scenarios start off with an invite and if so, just use this one.
     */
    protected SipMsgEvent defaultInviteEvent;

    protected TransactionId defaultInviteTransactionId;

    /**
     * Default 180 Ringing that is in the same transaction as the {@link #defaultInviteEvent}.
     */
    protected SipMsgEvent default180RingingEvent;

    /**
     * Default 200 OK that is in the same transaction as the {@link #defaultInviteEvent}.
     */
    protected SipMsgEvent default200OKEvent;

    /**
     * Since everything is pretty much set in this test class the {@link ActorContext} is always
     * starting off the same way as well, which is really just reflecting how the {@link PipeLine}
     * is configured.
     */
    protected ActorContext defaultCtx;

    protected MockActorSystem actorSystem;

    public InviteServerTransactionTestBase() throws Exception {
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
        timer = new MockTimer();

        first = new EventProxy();
        last = new EventProxy(responses);
        supervisor = new TransactionSupervisor(config);

        factory = PipeLineFactory.withDefaultChain(first, supervisor, last);
        actorSystem = new MockActorSystem(factory);

        defaultInviteEvent = SipMsgEvent.create(invite);
        defaultInviteTransactionId = getTransactionId(defaultInviteEvent);
        default180RingingEvent = SipMsgEvent.create(ringing);
        default200OKEvent = SipMsgEvent.create(twoHundredToInvite);

        defaultCtx = ActorContext.withPipeLine(0, actorSystem, factory.newPipeLine());
    }

    /**
     * Convenience method for running the first scheduled event on the scheduled job queue.
     */
    protected void runDelayedJob() {
        final DelayedJob scheduledEvent = this.actorSystem.scheduledJobs.remove(0);
        scheduledEvent.job.run();
    }

    /**
     * Convenience method for fireing a timer that is on the scheduled job queue.
     */
    protected void fireTimer(SipTimer timer) {
        int index = -1;
        int i = 0;
        while (index == -1 || i < actorSystem.scheduledJobs.size() ) {
            DelayedJob scheduledEvent = actorSystem.scheduledJobs.get(i);
            final Event event = scheduledEvent.job.getEvent();
            boolean found = false;
            switch (timer) {
                case Trying:
                    found = event.isSipTimer100Trying();
                    break;
                case A:
                    found = event.isSipTimerA();
                    break;
                case B:
                    found = event.isSipTimerB();
                    break;
                case C:
                    found = event.isSipTimerC();
                    break;
                case D:
                    found = event.isSipTimerD();
                    break;
                case E:
                    found = event.isSipTimerE();
                    break;
                case F:
                    found = event.isSipTimerF();
                    break;
                case G:
                    found = event.isSipTimerG();
                    break;
                case H:
                    found = event.isSipTimerH();
                    break;
                case I:
                    found = event.isSipTimerI();
                    break;
                case J:
                    found = event.isSipTimerJ();
                    break;
                case K:
                    found = event.isSipTimerK();
                    break;
                case L:
                    found = event.isSipTimerL();
                    break;
                default:
                    fail("Unkonwn timer type. Did you add one but forgot to update here?");
            }
            if (found) {
                index = i;
            }
            ++i;
        }
        if (index != -1) {
            actorSystem.scheduledJobs.remove(index).job.run();
        } else {
            fail("Did not find the timer in question");
        }
    }

    /**
     * Ensure that the desired timer has been scheduled and that it has ONLY been scheduled once.
     * <p/>
     * NOTE: we do not care in which order the timers have been scheduled, only that the timer
     * we are looking for has indeed been scheduled.
     *
     * @param delay
     * @param timer
     */
    protected void assertSheduledTimer(final Duration delay, final SipTimer timer) {
        int count = 0;
        for (DelayedJob scheduledEvent : this.actorSystem.scheduledJobs) {
            final Event event = scheduledEvent.job.getEvent();
            boolean found = false;
            switch (timer) {
                case Trying:
                    found = event.isSipTimer100Trying();
                    break;
                case A:
                    found = event.isSipTimerA();
                    break;
                case B:
                    found = event.isSipTimerB();
                    break;
                case C:
                    found = event.isSipTimerC();
                    break;
                case D:
                    found = event.isSipTimerD();
                    break;
                case E:
                    found = event.isSipTimerE();
                    break;
                case F:
                    found = event.isSipTimerF();
                    break;
                case G:
                    found = event.isSipTimerG();
                    break;
                case H:
                    found = event.isSipTimerH();
                    break;
                default:
                    fail("Unkonwn timer type. Did you add one but forgot to update here?");
            }

            if (found) {
                assertThat(scheduledEvent.delay, is(delay));
                found = false;
                ++count;
            }

        }

        if (count > 1) {
            fail("Timer " + timer + " has been scheduled multiple times. That shouldn't be.");
        }

        if (count == 0) {
            fail("Timer " + timer + " was not scheduled");
        }
    }


    /**
     * Helper method for making sure that the EventProxy Actor has received the responses for a
     * particular transaction. This method will only look at the responses but if you also want to
     * remove them then you should use
     * {@link #consumeResponses(EventProxy, TransactionId, Integer...)} instead.
     *
     * @param id
     * @param expectedResponses
     */
    protected void peekResponses(final EventProxy proxy, final TransactionId id, final Integer... expectedResponses) {
        consumeOrPeekResponses(false, proxy, id, expectedResponses);
    }

    protected void consumeResponses(final EventProxy proxy, final TransactionId id, final Integer... expectedResponses) {
        consumeOrPeekResponses(true, proxy, id, expectedResponses);
    }

    /**
     * Convenience method for asserting the responses and optionally also consuming them.
     *
     * @param consume
     * @param proxy
     * @param id
     * @param expectedResponses
     */
    protected void consumeOrPeekResponses(final boolean consume, final EventProxy proxy, final TransactionId id,
                                          final Integer... expectedResponses) {
        for (int i = 0; i < expectedResponses.length; ++i) {
            final SipMsgEvent event =
                    (consume ? proxy.responseEvents.remove(0) : proxy.responseEvents.get(i)).toSipMsgEvent();
            final SipMessage msg = event.getSipMessage();
            assertThat(msg.isResponse(), is(true));
            assertThat(getTransactionId(event), is(id));
            assertThat(msg.toResponse().getStatus(), is(expectedResponses[i]));
        }
    }

    /**
     * Ensure that the {@link EventProxy} has only received the expected number of requests.
     * <p/>
     * Note, as soon as you "consume" any requests those will disappear and as such the count
     * will always zero after you have consumed them.
     *
     * @param proxy    the proxy to check.
     * @param expected the expected number of requests.
     */
    protected void assertReceivedRequests(final EventProxy proxy, final int expected) {
        assertThat(proxy.requestEvents.size(), is(expected));
    }

    protected void peekRequests(final EventProxy proxy, final TransactionId id, final String... expectedRequests) {
        consumeOrPeekRequests(false, proxy, id, expectedRequests);
    }

    protected void consumeRequests(final EventProxy proxy, final TransactionId id, final String... expectedRequests) {
        consumeOrPeekRequests(true, proxy, id, expectedRequests);
    }

    protected void consumeOrPeekRequests(final boolean consume, final EventProxy proxy, final TransactionId id,
                                         final String... expectedRequests) {
        for (int i = 0; i < expectedRequests.length; ++i) {
            final SipMsgEvent event =
                    (consume ? proxy.requestEvents.remove(0) : proxy.requestEvents.get(i)).toSipMsgEvent();
            final SipMessage msg = event.getSipMessage();
            assertThat(msg.isRequest(), is(true));
            assertThat(getTransactionId(event), is(id));
            assertThat(msg.getMethod().toString(), is(expectedRequests[i]));
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
    protected void assertInitialInvite(final SipMsgEvent initialInvite) {
        // the transaction should be in the proceeding state
        assertServerTransactionState(initialInvite, TransactionState.PROCEEDING);

        // the "last" should have received the invite event that was pushed
        // through and nothing else.
        assertThat(this.last.requestEvents.size(), is(1));
        assertThat(this.last.requestEvents.get(0), is(initialInvite));
        assertThat(this.last.responseEvents.size(), is(0));

        // the "first" should have received the invite event as well
        // as a 100 Trying response...
        assertThat(this.first.requestEvents.size(), is(1));
        assertThat(this.first.requestEvents.get(0), is(initialInvite));

        assertThat(this.first.responseEvents.size(), is(1));
        assertThat(getSipMessage(this.first.responseEvents.get(0)).isResponse(), is(true));
        assertThat(getSipMessage(this.first.responseEvents.get(0)).toResponse().is100Trying(), is(true));

        this.first.reset();
        this.last.reset();
    }

    protected SipMessage getSipMessage(final Event event) {
        return ((SipMsgEvent) event).getSipMessage();
    }

    /**
     * Helper method for asserting the transaction state for the transaction identified by the sip
     * event.
     *
     * @param event
     * @param state
     */
    protected void assertServerTransactionState(final SipMsgEvent event, final TransactionState state) {
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

    protected TransactionId getTransactionId(final SipMsgEvent event) {
        return TransactionId.create(event.getSipMessage());
    }
}
