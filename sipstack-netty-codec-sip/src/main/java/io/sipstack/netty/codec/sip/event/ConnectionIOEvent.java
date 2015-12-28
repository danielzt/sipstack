package io.sipstack.netty.codec.sip.event;

import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.impl.IOEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ConnectionIOEvent extends IOEvent {

    @Override
    default ConnectionIOEvent toConnectionIOEvent() {
        return this;
    }

    @Override
    default boolean isConnectionIOEvent() {
        return true;
    }

}
