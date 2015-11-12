package io.sipstack.netty.codec.sip.event;

import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.impl.IOEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ConnectionIOEvent extends IOEvent {

    default boolean isConnectionIOEvent() {
        return true;
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
        return this;
    }

}
