package io.sipstack.transaction.impl;

import io.sipstack.ControllableClock;
import io.sipstack.SipStackTestBase;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.SystemClock;
import io.sipstack.transaction.TransactionLayer;
import io.sipstack.transport.FlowState;
import io.sipstack.transport.impl.FlowStorage;
import io.sipstack.transport.impl.MapBasedFlowStorage;
import org.junit.Before;

/**
 * @author jonas@jonasborjesson.com
 */
public class TransactionTestBase extends SipStackTestBase {

    protected TransactionLayerConfiguration config;
    protected final ControllableClock clock = new ControllableClock();

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
        final TransportLayerConfiguration transportConfig = new TransportLayerConfiguration();
        final FlowStorage flowStorage = new MapBasedFlowStorage(transportConfig, clock);
        transports = new MockTransportLayer(flowStorage, mockChannelContext, clock);

        transactionLayer = new DefaultTransactionLayer(transports, clock, defaultScheduler, config);

        transports.setChannelOutboundHandler(transactionLayer);
    }

    public void reset() {
        transports.reset();
        myApplication.reset();
        defaultScheduler.reset();
        mockChannelContext.reset();
    }




}
