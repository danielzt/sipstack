package io.sipstack.transport.impl;

import io.sipstack.MockChannelHandlerContext;
import io.sipstack.SipStackTestBase;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.SystemClock;
import io.sipstack.transport.FlowId;
import org.hamcrest.CoreMatchers;
import org.junit.Before;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jonas@jonasborjesson.com
 */
public class TransportLayerTestBase extends SipStackTestBase {

    protected Clock defaultClock;

    protected DefaultTransportLayer transportLayer;

    protected FlowStorage defaultFlowStorage;


    @Before
    public void setUp() throws Exception {
        super.setUp();
        defaultClock = new SystemClock();

        final TransportLayerConfiguration config = new TransportLayerConfiguration();
        config.getFlow().setDefaultStorageSize(100);

        defaultFlowStorage = new DefaultFlowStorage(config.getFlow());

        transportLayer = new DefaultTransportLayer(config, defaultClock, defaultFlowStorage, defaultScheduler);
        defaultChannelCtx = new MockChannelHandlerContext(transportLayer);
    }

    /**
     * Convenience method for making sure that a flow actually exists in the flow storage.
     * @param id
     */
    public void assertFlowExists(final FlowId id) {
        assertThat(defaultFlowStorage.get(id), not((FlowId)null));
    }

    public void assertFlowExists(final Connection connection) {
        assertFlowExists(FlowId.create(connection.id()));
    }

    public void assertFlowDoesNotExist(final FlowId id) {
        assertThat(defaultFlowStorage.get(id), is((FlowId)null));
    }

    public void assertFlowDoesNotExist(final Connection connection) {
        assertFlowDoesNotExist(FlowId.create(connection.id()));
    }
}
