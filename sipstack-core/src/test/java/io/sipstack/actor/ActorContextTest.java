/**
 * 
 */
package io.sipstack.actor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import io.pkts.buffer.Buffers;
import io.sipstack.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public class ActorContextTest {

    private BlockingQueue<Runnable> jobQueue;
    private Worker worker;
    private Thread t;
    private ActorSystem actorSystem;

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {

    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        this.actorSystem = mock(ActorSystem.class);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {}

    private void initWorker(final PipeLineFactory factory) {
        this.jobQueue = new LinkedBlockingQueue<Runnable>(10);
        this.worker = new Worker(0, this.jobQueue);
        this.t = new Thread(this.worker);
        this.t.start();
    }

    /**
     * Test so that an Actor can actually reverse the direction of a message.
     * 
     * Note that we create three actors and all of them should be receiving the inbound event.
     * However, since the third and last actor reverses the direction of the event, that actor
     * itself will not be getting the outbound event so therefore the outbound latch is one less
     * (i.e. two)
     * 
     * @throws Exception
     */
    @Test
    public void testBasicUpThenDownPropagation() throws Exception {
        final CountDownLatch inboundLatch = new CountDownLatch(5);
        // note index 2 is of course our 3rd actor
        final ActorContext ctx = prepare(3, inboundLatch, 2);
        ctx.forward(new DummyEvent());

        assertThat("One of more Actors did not get inbound event", inboundLatch.await(1000, TimeUnit.MILLISECONDS),
                is(true));
    }

    private ActorContext prepare(final CountDownLatch inboundLatch) {
        return prepare((int) inboundLatch.getCount(), inboundLatch, -1);
    }

    /**
     * 
     * @param inboundLatch
     * @param outboundLatch
     * @param reverseAtIndex the index of the actor who should reverse the direction of the event.
     *        If you pass in a number higher or lower than the latch count it would result in no
     *        actors reversing it.
     * @return
     */
    private ActorContext prepare(final int noOfActors, final CountDownLatch latch, final int reverseAtIndex) {

        final List<Actor> actors = new ArrayList<Actor>();
        for (int i = 0; i < noOfActors; ++i) {
            actors.add(new HelloActor(latch, reverseAtIndex == i));
        }
        final PipeLineFactory pipeLineFactory = PipeLineFactory.withDefaultChain(actors);
        initWorker(pipeLineFactory);

        final PipeLine pipe = pipeLineFactory.newPipeLine();
        return ActorContext.withPipeLine(this.actorSystem, pipe);
    }

    /**
     * Test so that an event is propagated correctly though the pipeline.
     * 
     * @throws Exception
     */
    @Test
    public void testBasicEventUpstreamPropagation() throws Exception {
        final CountDownLatch latch = new CountDownLatch(3);
        final ActorContext ctx = prepare(latch);
        final Event event = new DummyEvent();
        this.jobQueue.offer(new Runnable() {
            @Override
            public void run() {
                ctx.forward(event);
            }
        });
        assertThat("One of more Actors did not get event", latch.await(1000, TimeUnit.MILLISECONDS), is(true));
    }

    private static class DummyEvent implements Event {

        private final int count;

        public DummyEvent() {
            this(0);
        }

        public DummyEvent(final int count) {
            this.count = count;
        }

        public int getCount() {
            return this.count;
        }

        @Override
        public Key key() {
            return Key.withBuffer(Buffers.wrap(1));
        }

        @Override
        public long getArrivalTime() {
            // TODO Auto-generated method stub
            return 0;
        }

    }

    private static class HelloActor implements Actor {
        private final CountDownLatch latch;

        /**
         * Flag indicating whether this actor should reverse the direction of the event when it
         * receives it.
         */
        private boolean reverseEvent;

        private HelloActor(final CountDownLatch latch) {
            this.latch = latch;
        }

        private HelloActor(final CountDownLatch latch, final boolean reverseEvent) {
            this.latch = latch;
            this.reverseEvent = reverseEvent;
        }

        @Override
        public void onEvent(final ActorContext ctx, final Event event) {
            this.latch.countDown();
            (this.reverseEvent ? ctx.reverse() : ctx).forward(event);
        }

        @Override
        public Supervisor getSupervisor() {
            throw new RuntimeException("Unit Test Nope - Dont call me");
        }

        @Override
        public ActorRef self() {
            throw new RuntimeException("Unit Test Nope - Dont call me");
        }
    }

}
