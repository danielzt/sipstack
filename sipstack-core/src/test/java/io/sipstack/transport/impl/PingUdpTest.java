package io.sipstack.transport.impl;

import io.pkts.packet.sip.Transport;

/**
 * All tests are defined in {@link PingTest} but if you run this class
 * all those tests will run using UDP as the transport.
 *
 * @author jonas@jonasborjesson.com
 */
public class PingUdpTest extends PingTest {

    @Override
    public Transport getTransport() {
        return Transport.udp;
    }
}
