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
        final CountDownLatch inboundLatch = new CountDownLatch(3);
        final CountDownLatch outboundLatch = new CountDownLatch(2);
        // note index 2 is of course our 3rd actor
        final ActorContext ctx = prepare(3, inboundLatch, outboundLatch, 2, true);
        ctx.fireUpstreamEvent(new DummyEvent());

        assertThat("One of more Actors did not get inbound event", inboundLatch.await(1000, TimeUnit.MILLISECONDS),
                is(true));
        assertThat("One of more Actors did not get outbound event", outboundLatch.await(1000, TimeUnit.MILLISECONDS),
                is(true));
    }

    /**
     * Test so that downstream events are flowing the way they should.
     * 
     * @throws Exception
     */
    @Test
    public void testBasicEventDownstreamPropagation() throws Exception {
        final CountDownLatch latch = new CountDownLatch(3);
        final ActorContext ctx = prepareOutbound(latch);
        final Event event = new DummyEvent();
        ctx.fireDownstreamEvent(event);

        assertThat("One of more Actors did not get event", latch.await(1000, TimeUnit.MILLISECONDS), is(true));
    }

    /**
     * Prepare an {@link ActorContext} for an outbound default pipeline.
     * 
     * @param outboundLatch
     * @return
     */
    private ActorContext prepareOutbound(final CountDownLatch outboundLatch) {
        return prepare((int) outboundLatch.getCount(), new CountDownLatch((int) outboundLatch.getCount()),
                outboundLatch, -1, false);

    }

    private ActorContext prepareInbound(final CountDownLatch inboundLatch) {
        return prepare((int) inboundLatch.getCount(), inboundLatch, new CountDownLatch((int) inboundLatch.getCount()),
                -1, true);
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
    private ActorContext prepare(final int noOfActors, final CountDownLatch inboundLatch,
            final CountDownLatch outboundLatch, final int reverseAtIndex, final boolean isInbound) {

        final List<Actor> actors = new ArrayList<Actor>();
        for (int i = 0; i < noOfActors; ++i) {
            actors.add(new HelloActor(inboundLatch, outboundLatch, reverseAtIndex == i));
        }
        final PipeLineFactory pipeLineFactory = PipeLineFactory.withDefaultChain(actors);
        initWorker(pipeLineFactory);

        final PipeLine pipe = pipeLineFactory.newPipeLine();
        if (isInbound) {
            return ActorContext.withInboundPipeLine(this.actorSystem, pipe);
        } else {
            return ActorContext.withOutboundPipeLine(this.actorSystem, pipe);
        }
    }

    /**
     * Test so that an event is propagated correctly though the pipeline.
     * 
     * @throws Exception
     */
    @Test
    public void testBasicEventUpstreamPropagation() throws Exception {
        final CountDownLatch latch = new CountDownLatch(3);
        final ActorContext ctx = prepareInbound(latch);
        final Event event = new DummyEvent();
        this.jobQueue.offer(new Runnable() {
            @Override
            public void run() {
                ctx.fireUpstreamEvent(event);
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
        private final CountDownLatch outboundLatch;

        /**
         * Flag indicating whether this actor should reverse the direction of the event when it
         * receives it.
         */
        private boolean reverseEvent;

        private HelloActor(final CountDownLatch latch) {
            this.latch = latch;
            this.outboundLatch = null;
        }

        private HelloActor(final CountDownLatch latch, final CountDownLatch outboundLatch, final boolean reverseEvent) {
            this.latch = latch;
            this.reverseEvent = reverseEvent;
            this.outboundLatch = outboundLatch;
        }

        @Override
        public void onUpstreamEvent(final ActorContext ctx, final Event event) {
            this.latch.countDown();
            if (this.reverseEvent) {
                ctx.fireDownstreamEvent(event);
            } else {
                ctx.fireUpstreamEvent(event);
            }
        }

        @Override
        public void onDownstreamEvent(final ActorContext ctx, final Event event) {
            this.outboundLatch.countDown();
            ctx.fireDownstreamEvent(event);
        }

        @Override
        public Supervisor getSupervisor() {
            // TODO Auto-generated method stub
            return null;
        }
    }

}
