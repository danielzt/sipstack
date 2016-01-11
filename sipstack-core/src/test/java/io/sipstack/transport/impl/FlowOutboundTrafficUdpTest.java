package io.sipstack.transport.impl;


import io.pkts.packet.sip.Transport;

/**
 *
 * @author jonas@jonasborjesson.com
 */
public class FlowOutboundTrafficUdpTest extends FlowOutboundTrafficTest{

    public Transport getTransport() {
        return Transport.udp;
    }

}
