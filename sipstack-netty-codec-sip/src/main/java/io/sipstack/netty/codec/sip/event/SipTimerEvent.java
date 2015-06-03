/**
 * 
 */
package io.sipstack.netty.codec.sip.event;

import io.netty.channel.ChannelHandlerContext;
import io.sipstack.netty.codec.sip.SipTimer;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;


/**
 * @author jonas@jonasborjesson.com
 *
 */
public abstract class SipTimerEvent extends Event {

    private final Object key;

    private SipTimerEvent(final long arrivalTime, final Object key) {
        super(arrivalTime);
        this.key = key;
    }

    public abstract SipTimer timer();

    @Override
    public final boolean isSipTimerEvent() {
        return true;
    }

    public Object key() {
        return key;
    }

    public static Builder withTimer(final SipTimer timer) {
        ensureNotNull(timer);
        return new Builder(timer);
    }

    public static class Builder {
        private final SipTimer timer;
        private long timestamp;
        private Object key;

        private Builder(final SipTimer timer) {
            this.timer = timer;
        }

        /**
         * Each timer event is targeting a particular transaction or dialog, which
         * both have keys identifying which one we are addressing. Therefore, each
         * timer event needs to contain the key so that when the timer fires, we
         * can correctly dispatch the timer to the correct e.g. transaction.
         *
         * Note: perhaps we should make a "key" interface or something.
         *
         * Note2: when a sip timer fires, it does so within the context
         * of where it was created which means it will be delivered to
         * the same entity that created it, which means that for transactions,
         * the {@link io.sipstack.netty.codec.sip.transaction.TransactionLayer} will be
         * getting the timer delivered to it via the
         * {@link io.sipstack.netty.codec.sip.InboundOutboundHandlerAdapter#userEventTriggered(ChannelHandlerContext, Object)}
         *
         * @param key
         * @return
         */
        public Builder withKey(final Object key) {
            this.key = key;
            return this;
        }

        public Builder withArrivalTime(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public SipTimerEvent build() {
            ensureNotNull(key, "The Key for this SIP Timer Event cannot be null");
            timestamp = timestamp > 0L ? timestamp : System.currentTimeMillis();
            switch (this.timer) {
                case Trying:
                    return new Timer100Trying(timestamp, key);
                case A:
                    return new TimerA(timestamp, key);
                case B:
                    return new TimerB(timestamp, key);
                case C:
                    return new TimerC(timestamp, key);
                case D:
                    return new TimerD(timestamp, key);
                case E:
                    return new TimerE(timestamp, key);
                case F:
                    return new TimerF(timestamp, key);
                case G:
                    return new TimerG(timestamp, key);
                case H:
                    return new TimerH(timestamp, key);
                case I:
                    return new TimerI(timestamp, key);
                case J:
                    return new TimerJ(timestamp, key);
                case K:
                    return new TimerK(timestamp, key);
                case L:
                    return new TimerL(timestamp, key);
                default:
                    throw new RuntimeException("Don't know what you are talking about.");

            }
        }

        private static class Timer100Trying extends SipTimerEvent {
            private Timer100Trying(final long arrivalTime, final Object key) {
                super(arrivalTime, key);
            }

            @Override
            public boolean isSipTimer100Trying() {
                return true;
            }

            @Override
            public SipTimer timer() {
                return SipTimer.Trying;
            }
        }


        private static class TimerA extends SipTimerEvent {
            private TimerA(final long arrivalTime, final Object key) {
                super(arrivalTime, key);
            }

            @Override
            public boolean isSipTimerA() {
                return true;
            }

            @Override
            public SipTimer timer() {
                return SipTimer.A;
            }
        }

        private static class TimerB extends SipTimerEvent {
            private TimerB(final long arrivalTime, final Object key) {
                super(arrivalTime, key);
            }

            @Override
            public boolean isSipTimerB() {
                return true;
            }

            @Override
            public SipTimer timer() {
                return SipTimer.B;
            }
        }

        private static class TimerC extends SipTimerEvent {
            private TimerC(final long arrivalTime, final Object key) {
                super(arrivalTime, key);
            }

            @Override
            public boolean isSipTimerC() {
                return true;
            }

            @Override
            public SipTimer timer() {
                return SipTimer.C;
            }
        }

        private static class TimerD extends SipTimerEvent {
            private TimerD(final long arrivalTime, final Object key) {
                super(arrivalTime, key);
            }

            @Override
            public boolean isSipTimerD() {
                return true;
            }

            @Override
            public SipTimer timer() {
                return SipTimer.D;
            }
        }

        private static class TimerE extends SipTimerEvent {
            private TimerE(final long arrivalTime, final Object key) {
                super(arrivalTime, key);
            }

            @Override
            public boolean isSipTimerE() {
                return true;
            }

            @Override
            public SipTimer timer() {
                return SipTimer.E;
            }
        }

        private static class TimerF extends SipTimerEvent {
            private TimerF(final long arrivalTime, final Object key) {
                super(arrivalTime, key);
            }

            @Override
            public boolean isSipTimerF() {
                return true;
            }

            @Override
            public SipTimer timer() {
                return SipTimer.F;
            }
        }

        private static class TimerG extends SipTimerEvent {
            private TimerG(final long arrivalTime, final Object key) {
                super(arrivalTime, key);
            }

            @Override
            public boolean isSipTimerG() {
                return true;
            }

            @Override
            public SipTimer timer() {
                return SipTimer.G;
            }
        }

        private static class TimerH extends SipTimerEvent {
            private TimerH(final long arrivalTime, final Object key) {
                super(arrivalTime, key);
            }

            @Override
            public boolean isSipTimerH() {
                return true;
            }

            @Override
            public SipTimer timer() {
                return SipTimer.H;
            }
        }

        private static class TimerI extends SipTimerEvent {
            private TimerI(final long arrivalTime, final Object key) {
                super(arrivalTime, key);
            }

            @Override
            public boolean isSipTimerI() {
                return true;
            }

            @Override
            public SipTimer timer() {
                return SipTimer.I;
            }
        }

        private static class TimerJ extends SipTimerEvent {
            private TimerJ(final long arrivalTime, final Object key) {
                super(arrivalTime, key);
            }

            @Override
            public boolean isSipTimerJ() {
                return true;
            }


            @Override
            public SipTimer timer() {
                return SipTimer.J;
            }
        }

        private static class TimerK extends SipTimerEvent {
            private TimerK(final long arrivalTime, final Object key) {
                super(arrivalTime, key);
            }

            @Override
            public boolean isSipTimerK() {
                return true;
            }

            @Override
            public SipTimer timer() {
                return SipTimer.K;
            }

        }

        private static class TimerL extends SipTimerEvent {
            private TimerL(final long arrivalTime, final Object key) {
                super(arrivalTime, key);
            }

            @Override
            public boolean isSipTimerL() {
                return true;
            }

            @Override
            public SipTimer timer() {
                return SipTimer.L;
            }

        }

    }

}
