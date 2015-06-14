package io.sipstack.netty.codec.sip.transaction;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.SipStackTestBase;
import io.sipstack.netty.codec.sip.SystemClock;
import io.sipstack.netty.codec.sip.config.TransactionLayerConfiguration;
import io.sipstack.netty.codec.sip.event.SipMessageEvent;
import org.junit.Before;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author jonas@jonasborjesson.com
 */
public class TransactionTestBase extends SipStackTestBase {

    protected TransactionLayer transactionLayer;
    protected TransactionLayerConfiguration config;
    protected final Clock clock = new SystemClock();


    @Before
    public void setUp() throws Exception {
        super.setUp();
        config = new TransactionLayerConfiguration();
        transactionLayer = new TransactionLayer(clock, defaultScheduler, config);
    }

    /**
     * Assert that there is no transaction for the given sip message.
     *
     * @param msg
     */
    public void assertNoTransaction(final SipMessage msg ) {
        final Optional<Transaction> transaction =  transactionLayer.getTransaction(TransactionId.create(msg));
        assertThat(transaction.isPresent(), is(false));
    }

    /**
     * Assert the state of a given transaction.
     *
     * @param msg
     * @param expectedState
     */
    public void assertTransactionState(final SipMessage msg, final TransactionState expectedState) {
        final Optional<Transaction> transaction =  transactionLayer.getTransaction(TransactionId.create(msg));
        assertThat(transaction.isPresent(), is(true));
        assertThat(transaction.get().state(), is(expectedState));
    }

    public SipMessageEvent createEvent(final SipMessage msg) {
        return new SipMessageEvent(defaultConnection, msg, 0);
    }

    /**
     * Convenience method for making sure that nothing was written out to the context
     * and as such, we didn't actually try and send something across the socket.
     */
    protected void assertNothingWritten() {
        assertThat(defaultChannelCtx.writeObjects.isEmpty(), is(true));
    }

    /**
     * Convenience method for making sure that nothing was forwarded in the
     * handler chain and as such the next one should not have gotten another
     * read event.
     */
    protected void assertNothingRead() {
        assertThat(defaultChannelCtx.channelReadObjects.isEmpty(), is(true));
    }

    /**
     * Convenience method for first hanging on the write latch and then
     * verify that a particular message was indeed written to the context.
     *
     * @param msg
     * @throws InterruptedException
     */
    protected void waitAndAssertMessageWritten(final Object msg) throws InterruptedException {
        defaultChannelCtx.writeLatch.get().await();
        if (msg instanceof SipMessage) {
            // convenience method since internally we will
            // always only write events
            assertMessageWritten(new SipMessageEvent(null, (SipMessage)msg, 0));
        } else {
            assertMessageWritten(msg);
        }
    }

    /**
     * Ensure that a particular object was indeed written to the context.
     *
     * @param msg
     */
    protected void assertMessageWritten(final Object msg) {
        assertThat(defaultChannelCtx.writeObjects.stream().filter(o -> o.equals(msg)).findFirst().isPresent(),
                is(true));
    }

    /**
     * Convenience method for hanging on the latch associated with the
     * channel handler context and then verify that the message was
     * indeed sent.
     *
     * @param msg
     */
    protected void waitAndAssertMessageForwarded(final Object msg) throws InterruptedException {
        defaultChannelCtx.fireChannelReadLatch.get().await();
        assertMessageForwarded(msg);
    }

    /**
     * When a channel handler processes a message (such as the invite server transaction)
     * it can choose to forward the message to the next handler in the pipeline. This
     * is a helper method to ensure that a particular message was indeed forwarded.
     * @param msg
     */
    protected void assertMessageForwarded(final Object msg) {
        assertThat(defaultChannelCtx.channelReadObjects.stream().filter(o -> o.equals(msg)).findFirst().isPresent(),
                is(true));
    }


}
