package io.sipstack.transaction.impl;

import io.hektor.core.ActorRef;
import io.pkts.packet.sip.header.SipHeader;
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
public class InviteServerTransactionActorTest extends SipStackTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Ensure that the invite server transaction transitions all the way from proceeding
     * til terminated following the happy path.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testBasicErrorResponseHandling() throws Exception {

        // instruct our test app to send back a 600...
        defaultInviteRequest.addHeader(SipHeader.create("X-Test-Respond", "600"));
        connection.resetLatch(2); // because we expect 100 and 600
        final SipMessageEvent invite = new DefaultSipMessageEvent(connection, defaultInviteRequest, 0);
        actor.tellAnonymously(invite);

        assertResponse("INVITE", 100);
        assertResponse("INVITE", 600);

        // we should have scheduled timer G and L so check that...
        assertTimerScheduled(SipTimer.G);
        assertTimerScheduled(SipTimer.H);

        // let's fire Timer G and we should be getting the 600 response again
        // Note, we want to reset the connection latch again so we can hang on
        // it while waiting for the response to be sent.
        connection.resetLatch(1);
        scheduler.fire(SipTimer.G);

        // make sure we got the 600 and a Timer G should
        // have been scheduled again and of course, Timer H
        // should still be configured.
        assertResponse("INVITE", 600);
        final MockCancellable timerG = assertTimerScheduled(SipTimer.G);
        final MockCancellable timerH = assertTimerScheduled(SipTimer.H);
        System.out.println("timer G ref is: " + timerG);

        // assert that the actor actually exists, which would be kind of
        // impossible that it didnt but still
        final ActorRef transactionActor = timerG.sender;
        assertThat(hektor.lookup(transactionActor.path()).isPresent(), is(true));


        // fire Timer H, which should take us over to the terminated state
        // and our actor should go off and die.
        scheduler.fire(SipTimer.H);

        // when we move over to the terminated state, ensure that
        // timer G actually has been cancelled...
        timerG.cancelLatch.await();

        // ... and our actor should be no more... Note, because we hang on
        // the cancellation of timer G that can only mean (hopefully) that
        // the Timer H has been processed and therefore our actor has been
        // cleaned up...
        // TODO: fix. no sleeps!
        Thread.sleep(100);
        assertThat(hektor.lookup(transactionActor.path()).isPresent(), is(false));
    }


    /**
     * Ensure that the invite server transaction transitions all the way from proceeding
     * til terminated following the happy path.
     *
     * @throws Exception
     */
    @Test(timeout = 500)
    public void testBasicLifeCycle() throws Exception {
        final SipMessageEvent invite = new DefaultSipMessageEvent(connection, defaultInviteRequest, 0);
        connection.resetLatch(2); // because we expect 100 and 600
        actor.tellAnonymously(invite);

        // Our app is sending a 200 OK to the INVITE so ensure that and
        // also note that the default config is to send a 100 Trying
        // right away
        assertResponse("INVITE", 100);
        assertResponse("INVITE", 200);

        // we should have scheduled timer L so check that...
        MockCancellable timerL = assertTimerScheduled(SipTimer.L);
        final ActorRef transactionActor = timerL.sender;
        assertThat(hektor.lookup(transactionActor.path()).isPresent(), is(true));

        scheduler.fire(SipTimer.L);

        // TODO: fix
        Thread.sleep(100);
        assertThat(hektor.lookup(transactionActor.path()).isPresent(), is(false));
    }
}