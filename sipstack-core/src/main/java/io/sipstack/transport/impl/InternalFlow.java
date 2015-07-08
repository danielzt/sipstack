package io.sipstack.transport.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.transport.Flow;

/**
 * @author jonas@jonasborjesson.com
 */
public interface InternalFlow extends Flow {

    // Connection connection();

    void write(SipMessage msg);
}
