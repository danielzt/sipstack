/**
 * 
 */
package io.sipstack.event;

import io.sipstack.actor.Key;

/**
 * Represents an {@link Event} in the system.
 * 
 * Note that all events that are being passed around will implement this interface. The reason for
 * this is that we are not building a generic Actor system but rather a very specific one tailored
 * to SIP only. Hence, even though it is very convenient to e.g. be able to send any type of Object
 * to any Actor (compare with akka.io, an really nice framework!) this system enforces SIP specific
 * events for type safety and performance reasons.
 * 
 * Note, an {@link Event} is internal to the sipstack implementation and should never ever be
 * exposed to the actual user application.
 * 
 * @author jonas@jonasborjesson.com
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

    default boolean isSipMsgEvent() {
        return false;
    }

    default boolean isTimerEvent() {
        return false;
    }

    default boolean isSipTimerEvent() {
        return false;
    }

    /**
     * There isn't an official "100 Trying" timer but if you look in the state machine for an Invite
     * Server Transaction it states that it should send a 100 Trying after 200 ms unless the TU does
     * so itself. Hence, this is the timer that keeps track of that.
     * 
     * @return
     */
    default boolean isSipTimer100Trying() {
        return false;
    }

    default boolean isSipTimerA() {
        return false;
    }

    default boolean isSipTimerB() {
        return false;
    }

    default boolean isSipTimerC() {
        return false;
    }

    default boolean isSipTimerD() {
        return false;
    }

    default boolean isSipTimerE() {
        return false;
    }

    default boolean isSipTimerF() {
        return false;
    }

    default boolean isSipTimerG() {
        return false;
    }

    default boolean isSipTimerH() {
        return false;
    }

    default boolean isSipTimerI() {
        return false;
    }

    default boolean isSipTimerJ() {
        return false;
    }

    default boolean isSipTimerK() {
        return false;
    }

    default boolean isSipTimerL() {
        return false;
    }

    default boolean isSipTimerM() {
        return false;
    }

    default TimerEvent toTimerEvent() {
        throw new ClassCastException("Cannot case " + getClass().getName() + " into a " + TimerEvent.class.getName());
    }

    default SipMsgEvent toSipMsgEvent() {
        throw new ClassCastException("Cannot case " + getClass().getName() + " into a " + SipMsgEvent.class.getName());
    }

    default IOEvent toIOEvent() {
        throw new ClassCastException("Cannot case " + getClass().getName() + " into a " + IOEvent.class.getName());
    }


}
