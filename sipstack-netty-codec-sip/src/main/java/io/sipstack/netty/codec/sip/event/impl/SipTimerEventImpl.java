package io.sipstack.netty.codec.sip.event.impl;

import io.netty.channel.ChannelHandlerContext;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.event.SipTimerEvent;
import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;

/**
 * @author jonas@jonasborjesson.com
 */
public abstract class SipTimerEventImpl extends IOEventImpl implements SipTimerEvent {

    public static final SipTimerEvent TIMEOUT = new TimerTimeout();
    public static final SipTimerEvent TRYING = new Timer100Trying();
    public static final SipTimerEvent A = new TimerA();
    public static final SipTimerEvent B = new TimerB();
    public static final SipTimerEvent C = new TimerC();
    public static final SipTimerEvent D = new TimerD();
    public static final SipTimerEvent E = new TimerE();
    public static final SipTimerEvent F = new TimerF();
    public static final SipTimerEvent G = new TimerF();
    public static final SipTimerEvent H = new TimerH();
    public static final SipTimerEvent I = new TimerI();
    public static final SipTimerEvent J = new TimerJ();
    public static final SipTimerEvent K = new TimerK();
    public static final SipTimerEvent L = new TimerL();
    public static final SipTimerEvent M = new TimerM();

    private SipTimerEventImpl() {
        super(null, 0L);
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

    public SipTimerEvent toSipTimerEvent() {
        return this;
    }

    private static class TimerTimeout extends SipTimerEventImpl {
        private TimerTimeout() {
        }

        @Override
        public boolean isSipTimerTimeout() {
            return true;
        }

        @Override
        public SipTimer timer() {
            return SipTimer.Timeout;
        }
    }

    private static class Timer100Trying extends SipTimerEventImpl {
        private Timer100Trying() {
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


    private static class TimerA extends SipTimerEventImpl {
        private TimerA() {
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

    private static class TimerB extends SipTimerEventImpl {
        private TimerB() {
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

    private static class TimerC extends SipTimerEventImpl {
        private TimerC() {
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

    private static class TimerD extends SipTimerEventImpl {
        private TimerD() {
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

    private static class TimerE extends SipTimerEventImpl {
        private TimerE() {
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

    private static class TimerF extends SipTimerEventImpl {
        private TimerF() {
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

    private static class TimerG extends SipTimerEventImpl {
        private TimerG() {
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

    private static class TimerH extends SipTimerEventImpl {
        private TimerH() {
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

    private static class TimerI extends SipTimerEventImpl {
        private TimerI() {
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

    private static class TimerJ extends SipTimerEventImpl {
        private TimerJ() {
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

    private static class TimerK extends SipTimerEventImpl {
        private TimerK() {
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

    private static class TimerL extends SipTimerEventImpl {
        private TimerL() {
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

    private static class TimerM extends SipTimerEventImpl {
        private TimerM() {
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
