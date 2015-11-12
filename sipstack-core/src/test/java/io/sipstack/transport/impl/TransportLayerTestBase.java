package io.sipstack.transport.impl;

import io.sipstack.SipStackTestBase;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.SystemClock;
import org.junit.After;
import org.junit.Before;

/**
 * @author jonas@jonasborjesson.com
 */
public class TransportLayerTestBase extends SipStackTestBase {

    protected Clock defaultClock;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        defaultClock = new SystemClock();
    }
}
