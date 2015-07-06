package io.sipstack.transport;

import io.pkts.packet.sip.SipMessage;

/**
 *
 * @author jonas@jonasborjesson.com
 */
public interface TransportUser {

    void onMessage(Flow flow, SipMessage msg);

    void onWriteCompleted(Flow flow, SipMessage msg);

    /**
     * Think IOException
     *
     * @param flow
     * @param msg
     */
    void onWriteFailed(Flow flow, SipMessage msg);
}
