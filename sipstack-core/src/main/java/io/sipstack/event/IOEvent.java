/**
 * 
 */
package io.sipstack.event;

/**
 * Generic class for all events concerning IO events.
 * 
 * @author jonas@jonasborjesson.com
 */
public interface IOEvent<T> extends Event {

    /**
     * Get the object that triggered this IOEvent to occur. Typically, this
     * could be a SIP message that was just read off of the network but
     * could also be an error when trying to write to a network socket.
     *
     * @return
     */
    T getObject();

    @Override
    default boolean isIOEvent() {
        return true;
    }

    @Override
    default IOEvent toIOEvent() {
        return this;
    }

    abstract class BaseIOEvent<T> implements IOEvent<T> {

        private final long arrivalTime;

        protected BaseIOEvent(final long arrivalTime) {
            this.arrivalTime = arrivalTime;
        }

        @Override
        public final long getArrivalTime() {
            return arrivalTime;
        }

    }

}
