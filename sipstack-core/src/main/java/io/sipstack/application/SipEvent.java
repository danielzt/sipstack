package io.sipstack.application;

import io.pkts.packet.sip.SipMessage;

/**
 * @author ajansson@twilio.com
 */
public interface SipEvent<T extends SipMessage> {

    T message();
}
