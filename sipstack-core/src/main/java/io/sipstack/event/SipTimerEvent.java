/**
 * 
 */
package io.sipstack.event;

import io.pkts.packet.sip.impl.PreConditions;
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
        // private Key key;
        private long timestamp;

        private Builder(final SipTimer timer) {
            this.timer = timer;
        }

        /*
        public Builder withKey(final Key key) {
            this.key = key;
            return this;
        }
        */

        public Builder withArrivalTime(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public SipTimerEvent build() {
            // PreConditions.ensureNotNull(key);
            switch (this.timer) {
                case Trying:
                    return new Timer100Trying(timestamp);
                case A:
                    return new TimerA(timestamp);
                case B:
                    return new TimerB(timestamp);
                case C:
                    return new TimerC(timestamp);
                case D:
                    return new TimerD(timestamp);
                case E:
                    return new TimerE(timestamp);
                case F:
                    return new TimerF(timestamp);
                case G:
                    return new TimerG(timestamp);
                case H:
                    return new TimerH(timestamp);
                case I:
                    return new TimerI(timestamp);
                case J:
                    return new TimerJ(timestamp);
                case K:
                    return new TimerK(timestamp);
                case L:
                    return new TimerL(timestamp);
                default:
                    throw new RuntimeException("Don't know what you are talking about.");

            }
        }

        private static class Timer100Trying extends AbstractEvent implements SipTimerEvent {
            private Timer100Trying(final long arrivalTime) {
                super(arrivalTime);
            }

            @Override
            public boolean isSipTimer100Trying() {
                return true;
            }
        }


        private static class TimerA extends AbstractEvent implements SipTimerEvent {
            private TimerA(final long arrivalTime) {
                super(arrivalTime);
            }

            @Override
            public boolean isSipTimerA() {
                return true;
            }
        }

        private static class TimerB extends AbstractEvent implements SipTimerEvent {
            private TimerB(final long arrivalTime) {
                super(arrivalTime);
            }

            @Override
            public boolean isSipTimerB() {
                return true;
            }
        }

        private static class TimerC extends AbstractEvent implements SipTimerEvent {
            private TimerC(final long arrivalTime) {
                super(arrivalTime);
            }

            @Override
            public boolean isSipTimerC() {
                return true;
            }
        }

        private static class TimerD extends AbstractEvent implements SipTimerEvent {
            private TimerD(final long arrivalTime) {
                super(arrivalTime);
            }

            @Override
            public boolean isSipTimerD() {
                return true;
            }
        }

        private static class TimerE extends AbstractEvent implements SipTimerEvent {
            private TimerE(final long arrivalTime) {
                super(arrivalTime);
            }

            @Override
            public boolean isSipTimerE() {
                return true;
            }
        }

        private static class TimerF extends AbstractEvent implements SipTimerEvent {
            private TimerF(final long arrivalTime) {
                super(arrivalTime);
            }

            @Override
            public boolean isSipTimerF() {
                return true;
            }
        }

        private static class TimerG extends AbstractEvent implements SipTimerEvent {
            private TimerG(final long arrivalTime) {
                super(arrivalTime);
            }

            @Override
            public boolean isSipTimerG() {
                return true;
            }
        }

        private static class TimerH extends AbstractEvent implements SipTimerEvent {
            private TimerH(final long arrivalTime) {
                super(arrivalTime);
            }

            @Override
            public boolean isSipTimerH() {
                return true;
            }
        }

        private static class TimerI extends AbstractEvent implements SipTimerEvent {
            private TimerI(final long arrivalTime) {
                super(arrivalTime);
            }

            @Override
            public boolean isSipTimerI() {
                return true;
            }
        }

        private static class TimerJ extends AbstractEvent implements SipTimerEvent {
            private TimerJ(final long arrivalTime) {
                super(arrivalTime);
            }

            @Override
            public boolean isSipTimerJ() {
                return true;
            }
        }

        private static class TimerK extends AbstractEvent implements SipTimerEvent {
            private TimerK(final long arrivalTime) {
                super(arrivalTime);
            }

            @Override
            public boolean isSipTimerK() {
                return true;
            }
        }

        private static class TimerL extends AbstractEvent implements SipTimerEvent {
            private TimerL(final long arrivalTime) {
                super(arrivalTime);
            }

            @Override
            public boolean isSipTimerL() {
                return true;
            }
        }

    }

}
