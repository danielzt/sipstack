package io.sipstack.transaction.impl;

import io.hektor.core.ActorRef;
import io.sipstack.MockCancellable;
import io.sipstack.SipStackTestBase;
import io.sipstack.netty.codec.sip.DefaultSipMessageEvent;
import io.sipstack.netty.codec.sip.SipMessageEvent;
import io.sipstack.timers.SipTimer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * @author jonas@jonasborjesson.com
 */
public class NonInviteServerTransactionActorTest extends SipStackTestBase {


    @Before
    public void setUp() throws Exception {
        super.setUp();

    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test the basic transition from trying->completed->terminated and ensure
     * that the actor has been removed from memory once we reach terminated.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testBasicLifeCycle() throws Exception {
        final SipMessageEvent bye = new DefaultSipMessageEvent(connection, defaultByeRequest, 0);
        actor.tellAnonymously(bye);

        // Our app is sending a 200 OK to the bye so ensure that.
        assertResponse("BYE", 200);

        // once we transition over to the completed state, we should
        // schedule timer J so check that.
        final MockCancellable scheduledTask = assertTimerScheduled(SipTimer.J);

        // the Cancellable contains the actor that scheduled the
        // task which will be the transaction actor we are testing.
        final ActorRef transactionActor = scheduledTask.sender;

        // Ensure that we can look it up in Hektor
        assertThat(hektor.lookup(transactionActor.path()).isPresent(), is(true));

        // fire the event, which is our Timer J event, which should move this
        // actor over to the terminated state and once there the actor will
        // be purged from memory...
        scheduler.fire(0);

        // ...so ensure that our actor is no longer present.
        // TODO: no sleeps!
        Thread.sleep(100);
        assertThat(hektor.lookup(transactionActor.path()).isPresent(), is(false));
    }

    /**
     * If a re-transmitted request is detected, it will be consumed and the last response
     * will be sent back again.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testRetransmission() throws Exception {
        final SipMessageEvent bye = new DefaultSipMessageEvent(connection, defaultByeRequest, 0);
        actor.tellAnonymously(bye);
        assertResponse("BYE", 200);

        // we are going to re-transmit the BYE request so therefore
        // we want to make sure that the latest response is also
        // re-sent and for that we want to reset the latch on the
        // fake connection object first.
        connection.resetLatch(1);

        actor.tellAnonymously(bye);
        assertResponse("BYE", 200);

        // there should have been a total of two messages written to the connection
        // now.
        assertThat(connection.count(), is(2));

        // note, ENSURE that not a second Timer J is scheduled just because of
        // the re-transmitted BYE request.
        assertThat(scheduler.countCurrentTasks(), is(1));
    }
}