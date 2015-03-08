/**
 * 
 */
package io.sipstack.actor;

import io.pkts.packet.sip.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public interface Event {

    Key key();

    /**
     * The arrival time of this {@link Event}. For incoming messages that will be the time they was
     * processed by the network stack. For outgoing messages, this will be the time at which the
     * message was created.
     * 
     * @return
     */
    long getArrivalTime();

    default boolean isSipMessage() {
        return false;
    }

    /**
     * If this {@link Event} is a {@link SipEvent} as reported by {@link #isSipMessage()} then you
     * can fetch the actual underlying {@link SipMessage} through this method. Note, if this is not
     * a {@link SipEvent} then this will always return null.
     * 
     * @return
     */
    default SipMessage getSipMessage() {
        return null;
    }

}
