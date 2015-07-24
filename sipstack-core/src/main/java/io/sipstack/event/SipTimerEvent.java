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
    private final ChannelHandlerContext ctx;

    private SipTimerEvent(final ChannelHandlerContext ctx, final Object key) {
        this.ctx = ctx;
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

    public ChannelHandlerContext ctx() {
        return ctx;
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
        private ChannelHandlerContext ctx;
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

        public Builder withContext(final ChannelHandlerContext ctx) {
            this.ctx = ctx;
            return this;
        }

        public SipTimerEvent build() {
            ensureNotNull(key, "The Key for this SIP Timer Event cannot be null");
            ensureNotNull(ctx, "The ChannelHanlderContext cannot be null");
            switch (this.timer) {
                case Trying:
                    return new Timer100Trying(ctx, key);
                case A:
                    return new TimerA(ctx, key);
                case B:
                    return new TimerB(ctx, key);
                case C:
                    return new TimerC(ctx, key);
                case D:
                    return new TimerD(ctx, key);
                case E:
                    return new TimerE(ctx, key);
                case F:
                    return new TimerF(ctx, key);
                case G:
                    return new TimerG(ctx, key);
                case H:
                    return new TimerH(ctx, key);
                case I:
                    return new TimerI(ctx, key);
                case J:
                    return new TimerJ(ctx, key);
                case K:
                    return new TimerK(ctx, key);
                case L:
                    return new TimerL(ctx, key);
                case M:
                    return new TimerM(ctx, key);
                default:
                    throw new RuntimeException("Don't know what you are talking about.");
            }
        }

        private static class Timer100Trying extends SipTimerEvent {
            private Timer100Trying(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
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
            private TimerA(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
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
            private TimerB(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
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
            private TimerC(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
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
            private TimerD(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
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
            private TimerE(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
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
            private TimerF(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
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
            private TimerG(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
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
            private TimerH(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
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
            private TimerI(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
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
            private TimerJ(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
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
            private TimerK(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
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
            private TimerL(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
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

        private static class TimerM extends SipTimerEvent {
            private TimerM(final ChannelHandlerContext ctx, final Object key) {
                super(ctx, key);
            }

            @Override
            public boolean isSipTimerM() {
                return true;
            }

            @Override
            public SipTimer timer() {
                return SipTimer.M;
            }

        }

    }

}
