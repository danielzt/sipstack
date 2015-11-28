package io.sipstack.transport.impl;

import io.sipstack.netty.codec.sip.Transport;

/**
 * All tests are defined in {@link PingTest} but if you run this class
 * all those tests will run using TCP as the transport.
 *
 * @author jonas@jonasborjesson.com
 */
public class PingTcpTest extends PingTest {
    @Override
    public Transport getTransport() {
        return Transport.tcp;
    }
}
