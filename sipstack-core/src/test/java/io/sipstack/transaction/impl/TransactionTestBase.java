package io.sipstack.transaction.impl;

import io.sipstack.SipStackTestBase;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.SystemClock;
import org.junit.Before;

/**
 * @author jonas@jonasborjesson.com
 */
public class TransactionTestBase extends SipStackTestBase {

    protected TransactionLayerConfiguration config;
    protected final Clock clock = new SystemClock();

    protected MockTransactionUser myApplication;

    protected MockTransportLayer transports;

    /**
     * The Transaction Layer is implementing the {@link TransportUser} interface
     * which is how we will "give/send" messages through that layer. Then, on the other
     * side, as in the upper part of the stack, the Transaction Layer is exposing the
     * {@link MockTransactionUser} interface which is how we are able to send messages
     * out of the stack (and through the transaction layer) again.
     */
    protected DefaultTransactionLayer transactionLayer;


    @Before
    public void setUp() throws Exception {
        super.setUp();
        config = new TransactionLayerConfiguration();
        // no need to consume a tonnes of memory for our unit tests
        config.setDefaultStorageSize(100);

        // for all tests in general we send the 100 trying right
        // away
        config.setSend100TryingImmediately(true);

        myApplication = new MockTransactionUser();
        transports = new MockTransportLayer();

        throw new RuntimeException("In the middle of re-writing this again");
        // transactionLayer = new DefaultTransactionLayer(clock, defaultScheduler, myApplication, config);
        // transactionLayer.start(transports);

        // myApplication.start(transactionLayer);
    }

    public void reset() {
        transports.reset();
        myApplication.reset();
        defaultScheduler.reset();
    }




}
