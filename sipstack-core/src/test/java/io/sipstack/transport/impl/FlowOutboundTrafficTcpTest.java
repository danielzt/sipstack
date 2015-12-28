package io.sipstack.transport.impl;

import io.sipstack.netty.codec.sip.Transport;

/**
 *
 * @author jonas@jonasborjesson.com
 */
public class FlowOutboundTrafficTcpTest extends FlowOutboundTrafficTest{

    public Transport getTransport() {
        return Transport.tcp;
    }

}
