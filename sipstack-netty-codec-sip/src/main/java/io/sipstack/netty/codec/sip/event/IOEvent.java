package io.sipstack.netty.codec.sip.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.impl.SipRequestIOEventImpl;
import io.sipstack.netty.codec.sip.event.impl.SipResponseIOEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface IOEvent {

    /**
     * The {@link Connection} over which this event took place.
     *
     * @return
     */
    Connection connection();

    /**
     * The time at which this event took place. If the event came off of the network
     * then this is the time at which that event had been read off of the socket.
     *
     * @return
     */
    long arrivalTime();

    /**
     * Events concerning the state of a connection will be delivered via ConnectionIOEvents.
     * Check if this IOEvent is a connection event.
     *
     * @return
     */
    default boolean isConnectionIOEvent() {
        return false;
    }

    default boolean isConnectionOpenedIOEvent() {
        return false;
    }

    default boolean isConnectionClosedIOEvent() {
        return false;
    }

    default boolean isConnectionCloseIOEvent() {
        return false;
    }

    default boolean isConnectionActiveIOEvent() {
        return false;
    }

    default boolean isConnectionInactiveIOEvent() {
        return false;
    }

    default boolean isConnectionBoundIOEvent() {
        return false;
    }

    default ConnectionIOEvent toConnectionIOEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + ConnectionIOEvent.class.getName());
    }

    /**
     * Check whether or not the event is a {@link SipMessageIOEvent}.
     *
     * @return
     */
    default boolean isSipMessageIOEvent() {
        return false;
    }

    /**
     * "Cast" this {@link IOEvent} into a {@link SipMessageIOEvent}.
     *
     * Use this method instead of actually casting the object.`
     *
     * @return
     */
    default SipMessageIOEvent toSipMessageIOEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipMessageIOEvent.class.getName());
    }

    /**
     * Check whether or not this {@link IOEvent} is a {@link SipRequestIOEvent}.
     *
     * @return
     */
    default boolean isSipRequestIOEvent() {
        return false;
    }

    default SipRequestIOEvent toSipRequestIOEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipRequestIOEvent.class.getName());
    }

    /**
     * Check whether or not this {@link IOEvent} is a {@link SipResponseIOEvent}.
     *
     * @return
     */
    default boolean isSipResponseIOEvent() {
        return false;
    }

    default SipResponseIOEvent toSipResponseIOEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipResponseIOEvent.class.getName());
    }

    default boolean isPingMessageIOEvent() {
        return false;
    }

    default boolean isPongMessageIOEvent() {
        return false;
    }

    static SipMessageIOEvent create(final Connection connection, final SipMessage msg) {
        if (msg.isRequest()) {
            return create(connection, msg.toRequest());
        }

        return create(connection, msg.toResponse());
    }
    /**
     * Factory method for creating a new {@link SipResponseIOEvent}.
     *
     * @param connection
     * @param response
     * @return
     */
    static SipResponseIOEvent create(final Connection connection, final SipResponse response) {
        return new SipResponseIOEventImpl(connection, response, System.currentTimeMillis());
    }

    /**
     * Factory method for creating a new {@link SipRequestIOEvent}.
     *
     * @param connection
     * @param request
     * @return
     */
    static SipRequestIOEvent create(final Connection connection, final SipRequest request) {
        return new SipRequestIOEventImpl(connection, request, System.currentTimeMillis());
    }

    default SipTimerEvent toSipTimerEvent() {
        throw new ClassCastException("Cannot case " + getClass().getName() + " into a " + SipTimerEvent.class.getName());
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

    /**
     * This is a generic timeout event for those times when there isn't a specific timer
     * event defined in any of the RFC:s.
     *
     * @return
     */
    default boolean isSipTimerTimeout() {
        return false;
    }

    default boolean isSipTimerTimeout1() {
        return false;
    }

    default boolean isSipTimerTimeout2() {
        return false;
    }

    default boolean isSipTimerTimeout3() {
        return false;
    }

    default boolean isSipTimerTimeout4() {
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
}
