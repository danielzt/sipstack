package io.sipstack.transaction.impl;

import io.sipstack.SipStackTestBase;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.SystemClock;
import io.sipstack.transaction.TransactionLayer;
import org.junit.Before;

/**
 * @author jonas@jonasborjesson.com
 */
public class TransactionTestBase extends SipStackTestBase {

    protected TransactionLayerConfiguration config;
    protected final Clock clock = new SystemClock();

    protected MockTransactionUser myApplication;

    protected MockTransportLayer transports;

    protected MockChannelHandlerContext mockChannelContext;

    /**
     * The {@link TransactionLayer} is really just another Netty
     * inbound/outbound handler so we will simply be sending events
     * up and down the chain to simulate the network.
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

        mockChannelContext = new MockChannelHandlerContext();
        transports = new MockTransportLayer(mockChannelContext);

        transactionLayer = new DefaultTransactionLayer(transports, new SystemClock(), defaultScheduler, config);

        transports.setChannelOutboundHandler(transactionLayer);
    }

    public void reset() {
        transports.reset();
        myApplication.reset();
        defaultScheduler.reset();
        mockChannelContext.reset();
    }




}
