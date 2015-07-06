/**
 * 
 */
package io.sipstack.event;

import io.netty.channel.ChannelHandlerContext;
import io.sipstack.net.InboundOutboundHandlerAdapter;
import io.sipstack.netty.codec.sip.SipTimer;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;


/**
 * @author jonas@jonasborjesson.com
 *
 */
public abstract class SipTimerEvent extends Event {

    private final Object key;

    private SipTimerEvent(final Object key) {
        this.key = key;
    }

    public abstract SipTimer timer();

    @Override
    public boolean equals(final Object other) {
        try {
            final SipTimerEvent otherTimer = (SipTimerEvent)other;
            return timer() == otherTimer.timer();
        } catch (final ClassCastException e) {
            return false;
        }
    }

    @Override
    public final boolean isSipTimerEvent() {
        return true;
    }

    public Object key() {
        return key;
    }

    public SipTimerEvent toSipTimerEvent() {
        return this;
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
         * Each timer event is targeting a particular io.sipstack.transaction.transaction or dialog, which
         * both have keys identifying which one we are addressing. Therefore, each
         * timer event needs to contain the key so that when the timer fires, we
         * can correctly dispatch the timer to the correct e.g. io.sipstack.transaction.transaction.
         *
         * Note: perhaps we should make a "key" interface or something.
         *
         * Note2: when a sip timer fires, it does so within the context
         * of where it was created which means it will be delivered to
         * the same entity that created it, which means that for transactions,
         * the {@link io.sipstack.netty.codec.sip.transaction.TransactionLayer} will be
         * getting the timer delivered to it via the
         * {@link InboundOutboundHandlerAdapter#userEventTriggered(ChannelHandlerContext, Object)}
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
            switch (this.timer) {
                case Trying:
                    return new Timer100Trying(key);
                case A:
                    return new TimerA(key);
                case B:
                    return new TimerB(key);
                case C:
                    return new TimerC(key);
                case D:
                    return new TimerD(key);
                case E:
                    return new TimerE(key);
                case F:
                    return new TimerF(key);
                case G:
                    return new TimerG(key);
                case H:
                    return new TimerH(key);
                case I:
                    return new TimerI(key);
                case J:
                    return new TimerJ(key);
                case K:
                    return new TimerK(key);
                case L:
                    return new TimerL(key);
                default:
                    throw new RuntimeException("Don't know what you are talking about.");

            }
        }

        private static class Timer100Trying extends SipTimerEvent {
            private Timer100Trying(final Object key) {
                super(key);
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
            private TimerA(final Object key) {
                super(key);
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
            private TimerB(final Object key) {
                super(key);
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
            private TimerC(final Object key) {
                super(key);
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
            private TimerD(final Object key) {
                super(key);
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
            private TimerE(final Object key) {
                super(key);
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
            private TimerF(final Object key) {
                super(key);
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
            private TimerG(final Object key) {
                super(key);
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
            private TimerH(final Object key) {
                super(key);
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
            private TimerI(final Object key) {
                super(key);
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
            private TimerJ(final Object key) {
                super(key);
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
            private TimerK(final Object key) {
                super(key);
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
            private TimerL(final Object key) {
                super(key);
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
