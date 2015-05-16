/**
 * 
 */
package io.sipstack.event;

import io.pkts.packet.sip.impl.PreConditions;

import java.time.Duration;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public interface TimerEvent extends Event {

    @Override
    default boolean isTimerEvent() {
        return true;
    }

    Object getEvent();

    Duration getDelay();

    @Override
    default TimerEvent toTimerEvent() {
        return this;
    }

    /**
     * 
     */
    interface TimerRef {

        boolean cancel();

        boolean isCancelled();

    }

    static Builder withDelay(final Duration delay) {
        PreConditions.ensureNotNull(delay);
        final Builder b = new Builder();
        return b.withDelay(delay);
    }

    static Builder withEvent(final Object event) {
        PreConditions.ensureNotNull(event);
        final Builder b = new Builder();
        return b.withEvent(event);
    }

    class Builder {

        private Duration delay;
        private Object event;

        private Builder() {
            // left empty intentionally
        }

        public Builder withDelay(final Duration delay) {
            this.delay = delay;
            return this;
        }

        public Builder withEvent(final Object event) {
            this.event = event;
            return this;
        }

        public TimerEvent build() {
            PreConditions.ensureNotNull(delay);
            PreConditions.ensureNotNull(event);
            return new DefaultTimerEvent(delay, event);


        }

    }

    class DefaultTimerEvent implements TimerEvent {
        private final Duration delay;
        private final Object event;

        private DefaultTimerEvent(final Duration delay, final Object event) {
            this.delay = delay;
            this.event = event;
        }

        @Override
        public Object getEvent() {
            return this.event;
        }

        @Override
        public Duration getDelay() {
            return this.delay;
        }

        @Override
        public long getArrivalTime() {
            // return event.getArrivalTime();
            return 0;
        }

    }


}
