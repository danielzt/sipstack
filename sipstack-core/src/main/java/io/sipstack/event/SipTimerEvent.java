/**
 * 
 */
package io.sipstack.event;

import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.actor.Key;
import io.sipstack.timers.SipTimer;


/**
 * @author jonas@jonasborjesson.com
 *
 */
public interface SipTimerEvent extends Event {

    @Override
    default boolean isSipTimerEvent() {
        return true;
    }

    static Builder withTimer(final SipTimer timer) {
        PreConditions.ensureNotNull(timer);
        return new Builder(timer);
    }

    class Builder {
        private final SipTimer timer;
        private Key key;
        private long timestamp;

        private Builder(final SipTimer timer) {
            this.timer = timer;
        }

        public Builder withKey(final Key key) {
            this.key = key;
            return this;
        }

        public Builder withArrivalTime(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public SipTimerEvent build() {
            PreConditions.ensureNotNull(key);
            switch (this.timer) {
                case Trying:
                    return new Timer100Trying(key, timestamp);
                case A:
                    return new TimerA(key, timestamp);
                case B:
                    return new TimerB(key, timestamp);
                case C:
                    return new TimerC(key, timestamp);
                case D:
                    return new TimerD(key, timestamp);
                case E:
                    return new TimerE(key, timestamp);
                case F:
                    return new TimerF(key, timestamp);
                case G:
                    return new TimerG(key, timestamp);
                case H:
                    return new TimerH(key, timestamp);
                case I:
                    return new TimerI(key, timestamp);
                case J:
                    return new TimerJ(key, timestamp);
                case K:
                    return new TimerK(key, timestamp);
                case L:
                    return new TimerL(key, timestamp);
                default:
                    throw new RuntimeException("Don't know what you are talking about.");

            }
        }

        private static class Timer100Trying extends AbstractEvent implements SipTimerEvent {
            private Timer100Trying(final Key key, final long arrivalTime) {
                super(key, arrivalTime);
            }

            @Override
            public boolean isSipTimer100Trying() {
                return true;
            }
        }


        private static class TimerA extends AbstractEvent implements SipTimerEvent {
            private TimerA(final Key key, final long arrivalTime) {
                super(key, arrivalTime);
            }

            @Override
            public boolean isSipTimerA() {
                return true;
            }
        }

        private static class TimerB extends AbstractEvent implements SipTimerEvent {
            private TimerB(final Key key, final long arrivalTime) {
                super(key, arrivalTime);
            }

            @Override
            public boolean isSipTimerB() {
                return true;
            }
        }

        private static class TimerC extends AbstractEvent implements SipTimerEvent {
            private TimerC(final Key key, final long arrivalTime) {
                super(key, arrivalTime);
            }

            @Override
            public boolean isSipTimerC() {
                return true;
            }
        }

        private static class TimerD extends AbstractEvent implements SipTimerEvent {
            private TimerD(final Key key, final long arrivalTime) {
                super(key, arrivalTime);
            }

            @Override
            public boolean isSipTimerD() {
                return true;
            }
        }

        private static class TimerE extends AbstractEvent implements SipTimerEvent {
            private TimerE(final Key key, final long arrivalTime) {
                super(key, arrivalTime);
            }

            @Override
            public boolean isSipTimerE() {
                return true;
            }
        }

        private static class TimerF extends AbstractEvent implements SipTimerEvent {
            private TimerF(final Key key, final long arrivalTime) {
                super(key, arrivalTime);
            }

            @Override
            public boolean isSipTimerF() {
                return true;
            }
        }

        private static class TimerG extends AbstractEvent implements SipTimerEvent {
            private TimerG(final Key key, final long arrivalTime) {
                super(key, arrivalTime);
            }

            @Override
            public boolean isSipTimerG() {
                return true;
            }
        }

        private static class TimerH extends AbstractEvent implements SipTimerEvent {
            private TimerH(final Key key, final long arrivalTime) {
                super(key, arrivalTime);
            }

            @Override
            public boolean isSipTimerH() {
                return true;
            }
        }

        private static class TimerI extends AbstractEvent implements SipTimerEvent {
            private TimerI(final Key key, final long arrivalTime) {
                super(key, arrivalTime);
            }

            @Override
            public boolean isSipTimerI() {
                return true;
            }
        }

        private static class TimerJ extends AbstractEvent implements SipTimerEvent {
            private TimerJ(final Key key, final long arrivalTime) {
                super(key, arrivalTime);
            }

            @Override
            public boolean isSipTimerJ() {
                return true;
            }
        }

        private static class TimerK extends AbstractEvent implements SipTimerEvent {
            private TimerK(final Key key, final long arrivalTime) {
                super(key, arrivalTime);
            }

            @Override
            public boolean isSipTimerK() {
                return true;
            }
        }

        private static class TimerL extends AbstractEvent implements SipTimerEvent {
            private TimerL(final Key key, final long arrivalTime) {
                super(key, arrivalTime);
            }

            @Override
            public boolean isSipTimerL() {
                return true;
            }
        }

    }

}
