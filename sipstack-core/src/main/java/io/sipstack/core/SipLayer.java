package io.sipstack.core;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.transport.Flow;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipLayer {

    void onUpstream(Flow flow, SipMessage message);
}
