/**
 * 
 */
package io.sipstack.actor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;
import io.sipstack.transaction.impl.TransactionSupervisor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public class InviteServerTransactionTest extends SipTestBase {

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
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
     * Test so that an initial invite is handled correctly in that a new transaction is created, a
     * 100 Trying is sent out, the "last" actor receives the invite and the "first" actor indeed
     * receives the 100 Trying.
     * 
     */
    @Test
    public void testInitialInvite() {
        final EventProxy first = new EventProxy();
        final EventProxy last = new EventProxy();
        final TransactionSupervisor supervisor = new TransactionSupervisor();
        final PipeLineFactory factory = PipeLineFactory.withDefaultChain(first, supervisor, last);
        final SipEvent inviteEvent = SipEvent.create(this.invite);

        final ActorContext ctx = ActorContext.withInboundPipeLine(factory.newPipeLine());
        ctx.fireUpstreamEvent(inviteEvent);

        // the nice thing with the pipe line approach is that it takes place on the same thread
        // as initiated the ctx.fireUpstreamEvent, which makes it super easy to test...

        // the transaction should be in the proceeding state
        final Transaction transaction = supervisor.getTransaction(TransactionId.create(this.invite));
        assertThat(transaction.isServerTransaction(), is(true));
        assertThat(transaction.isClientTransaction(), is(false));
        assertThat(transaction.getState(), is(TransactionState.PROOCEEDING));

        // the "last" should have received the invite event that was pushed
        // through and nothing else.
        assertThat(last.upstreamEvents.size(), is(1));
        assertThat(last.upstreamEvents.get(0), is(inviteEvent));
        assertThat(last.downstreamEvents.size(), is(0));

        // the "first" should have received the invite event as well
        // as a 100 Trying response...
        assertThat(first.upstreamEvents.size(), is(1));
        assertThat(first.upstreamEvents.get(0), is(inviteEvent));

        assertThat(first.downstreamEvents.size(), is(1));
        assertThat(first.downstreamEvents.get(0).getSipMessage().isResponse(), is(true));
        assertThat(first.downstreamEvents.get(0).getSipMessage().toResponse().is100Trying(), is(true));
    }

}
