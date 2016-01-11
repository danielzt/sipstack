package io.sipstack.transport.impl;


import io.pkts.packet.sip.Transport;

/**
 * @author jonas@jonasborjesson.com
 */
public class LifeTimeTimerTcpTest extends LifeTimeTimerTest {

    @Override
    public Transport getTransport() {
        return Transport.tcp;
    }
}
