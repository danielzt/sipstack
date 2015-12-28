package io.sipstack.transport.impl;

import io.sipstack.config.SipConfiguration;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.Transport;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.transport.Flow;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Each flow has a maximum life time it can live if it doesn't
 * receive any traffic. If you have active ping enabled this is not
 * necessary but otherwise the life time timer is crucial so the flow
 * gets cleaned up at some point in time.
 *
 * These tests are focused on that...
 *
 * @author jonas@jonasborjesson.com
 */
public abstract class LifeTimeTimerTest extends TransportLayerTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    public abstract Transport getTransport();

    @Test
    public void testLifeTimeTimerFires() throws Exception {
        final Transport transport = getTransport();
        final TransportLayerConfiguration config =
                loadConfiguration(SipConfiguration.class, "LifetimeTest001.yaml").getTransport();
        assertThat("You changed the config file, this test assumes a 2 min life time timeout",
                config.getFlow().getTimeout().toMinutes(), is(2L));

        final Object[]  objects = initiateFlowToActive(config, transport);
        final Connection connection = (Connection)objects[0];
        final Flow flow = (Flow)objects[1];

        // 20 seconds pass and a request shows up and is processed
        defaultClock.plusSeconds(20);
        transportLayer.channelRead(defaultChannelCtx, IOEvent.create(connection, defaultInviteRequest));

        // now jump another 100 seconds, which then would fire the timer...
        defaultClock.plusSeconds(100);
        defaultScheduler.fire(SipTimer.Timeout);

        // but, since we received a message we should not transition over to
        // the closing state but rather schedule the timer again. Note, we will
        // use the timestamp of the last received event as the base since we
        // are trying to kill the flow if we haven't received anything in 2
        // minutes (configurable of course). Hence, in this test, we did receive
        // a message 100 seconds ago so we will schedule the timer for +20 seconds
        // in the future and when it fires again, there has now been 120 seconds
        // without any new messages and then we should close the flow...
        assertTimerScheduled(SipTimer.Timeout, Duration.ofSeconds(20));

        // so jump those extra 20 seconds and the flow should be killed now...
        defaultClock.plusSeconds(20);
        defaultScheduler.fire(SipTimer.Timeout);
    }

}
