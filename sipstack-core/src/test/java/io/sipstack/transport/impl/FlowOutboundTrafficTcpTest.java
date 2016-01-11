package io.sipstack.transport.impl;


import io.pkts.packet.sip.Transport;

/**
 *
 * @author jonas@jonasborjesson.com
 */
public class FlowOutboundTrafficTcpTest extends FlowOutboundTrafficTest{

    public Transport getTransport() {
        return Transport.tcp;
    }

}
