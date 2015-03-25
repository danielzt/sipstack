/**
 * 
 */
package io.sipstack.event;

import io.sipstack.actor.Key;
import io.sipstack.netty.codec.sip.Connection;

/**
 * Generic class for all events concerning IO events.
 * 
 * @author jonas@jonasborjesson.com
 */
public interface IOEvent extends Event {

    /**
     * The connection on which this IO event occured.
     * 
     * @return
     */
    Connection getConnection();

    @Override
    default boolean isIOEvent() {
        return true;
    }

    default boolean isReadEvent() {
        return false;
    }

    default boolean isSipReadEvent() {
        return false;
    }

    @Override
    default IOEvent toIOEvent() {
        return this;
    }

    static abstract class BaseIOEvent implements IOEvent {

        private final Connection connection;
        private final Key key;
        private final long arrivalTime;

        protected BaseIOEvent(final long arrivalTime, final Key key, final Connection connection) {
            this.arrivalTime = arrivalTime;
            this.key = key;
            this.connection = connection;
        }

        @Override
        public final Key key() {
            return this.key;
        }

        @Override
        public final long getArrivalTime() {
            return arrivalTime;
        }

        @Override
        public final Connection getConnection() {
            return this.connection;
        }

    }

}
