/**
 * 
 */
package io.sipstack.event;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.actor.Key;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.SipMessageEvent;

/**
 * An event representing a read event.
 * 
 * @author jonas@jonasborjesson.com
 */
public interface IOReadEvent<T> extends IOEvent {

    @Override
    default boolean isReadEvent() {
        return true;
    }

    /**
     * Get the object that was read off of the network.
     * 
     * @return
     */
    T getObject();

    static IOReadEvent<SipMessage> create(final SipMessageEvent event) {
        final Connection connection = event.getConnection();
        final Key key = Key.withConnectionId(connection.id());
        final long arrivalTime = event.getArrivalTime();
        final SipMessage msg = event.getMessage();
        return new IOSipReadEvent(arrivalTime, key, connection, msg);
    }

    static final class IOSipReadEvent extends IOEvent.BaseIOEvent implements IOReadEvent<SipMessage> {

        private final SipMessage msg;

        private IOSipReadEvent(final long arrivalTime, final Key key, final Connection connection, final SipMessage msg) {
            super(arrivalTime, key, connection);
            this.msg = msg;
        }

        @Override
        public boolean isSipReadEvent() {
            return true;
        }


        @Override
        public SipMessage getObject() {
            return msg;
        }

    }

}
