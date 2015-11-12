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
}
