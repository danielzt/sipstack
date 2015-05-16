/**
 * 
 */
package io.sipstack.event;

/**
 * @author jonas@jonasborjesson.com
 */
public abstract class AbstractEvent implements Event {
    // private final Key key;
    private final long arrivalTime;

    // protected AbstractEvent(final Key key, final long arrivalTime) {
        protected AbstractEvent(final long arrivalTime) {
        // this.key = key;
        this.arrivalTime = arrivalTime;
    }

    @Override
    public final long getArrivalTime() {
        return this.arrivalTime;
    }
}
