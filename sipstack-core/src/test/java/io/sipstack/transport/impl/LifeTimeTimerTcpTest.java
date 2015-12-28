package io.sipstack.transport.impl;

import io.sipstack.netty.codec.sip.Transport;

/**
 * @author jonas@jonasborjesson.com
 */
public class LifeTimeTimerTcpTest extends LifeTimeTimerTest {

    @Override
    public Transport getTransport() {
        return Transport.tcp;
    }
}
