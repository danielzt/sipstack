/**
 * 
 */
package io.sipstack.event;

import io.sipstack.actor.Key;

/**
 * Represents an {@link Event} in the system.
 * 
 * Note, an {@link Event} is internal to the sipstack implementation and should never ever be
 * exposed to the actual user application.
 * 
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

    default boolean isIOEvent() {
        return false;
    }

    default boolean isSipEvent() {
        return false;
    }

    default boolean isTimerEvent() {
        return false;
    }

    default TimerEvent toTimerEvent() {
        throw new ClassCastException("Cannot case " + getClass().getName() + " into a " + TimerEvent.class.getName());
    }

    default SipEvent toSipEvent() {
        throw new ClassCastException("Cannot case " + getClass().getName() + " into a " + SipEvent.class.getName());
    }

    default IOEvent toIOEvent() {
        throw new ClassCastException("Cannot case " + getClass().getName() + " into a " + IOEvent.class.getName());
    }


}
