package io.sipstack.netty.codec.sip.transaction;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.MockChannelHandlerContext;
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
        // no need to consume a tonnes of memory for our unit tests
        config.setDefaultStorageSize(100);
        resetTransactionLayer();
        defaultChannelCtx = new MockChannelHandlerContext(transactionLayer);
    }

    public void resetTransactionLayer() {
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
     * Convenience method for first hanging on the write latch and then
     * verify that a particular message was indeed written to the context.
     *
     * @param msg
     * @throws InterruptedException
     */
    protected void waitAndAssertMessageWritten(final Object msg) throws InterruptedException {
        defaultChannelCtx.writeLatch().await();
        if (msg instanceof SipMessage) {
            // convenience method since internally we will
            // always only write events
            defaultChannelCtx.assertMessageWritten(new SipMessageEvent(null, (SipMessage)msg, 0));
        } else {
            defaultChannelCtx.assertMessageWritten(msg);
        }
    }

    /**
     * Convenience method for hanging on the latch associated with the
     * channel handler context and then verify that the message was
     * indeed sent.
     *
     * @param msg
     */
    protected void waitAndAssertMessageForwarded(final Object msg) throws InterruptedException {
        defaultChannelCtx.fireChannelReadLatch().await();
        defaultChannelCtx.assertMessageForwarded(msg);
    }


}
