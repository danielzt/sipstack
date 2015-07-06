package io.sipstack.transport;

import io.pkts.packet.sip.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransportUser {

    void onRequest(Flow flow, SipMessage msg);

    void onResponse(Flow flow, SipMessage msg);
}
