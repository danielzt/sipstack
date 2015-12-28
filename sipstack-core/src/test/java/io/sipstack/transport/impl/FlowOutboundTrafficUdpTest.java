package io.sipstack.transport.impl;

import io.sipstack.netty.codec.sip.Transport;

/**
 *
 * @author jonas@jonasborjesson.com
 */
public class FlowOutboundTrafficUdpTest extends FlowOutboundTrafficTest{

    public Transport getTransport() {
        return Transport.udp;
    }

}
