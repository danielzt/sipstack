package io.sipstack.netty.codec.sip.event;

import io.sipstack.netty.codec.sip.SipTimer;
import io.sipstack.netty.codec.sip.event.impl.IOEventImpl;
import io.sipstack.netty.codec.sip.event.impl.SipTimerEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipTimerEvent extends IOEvent {

    SipTimer timer();

    default boolean isSipTimerEvent() {
        return true;
    }

    static SipTimerEvent create(final SipTimer timer) {
        switch (timer) {
            case Trying:
                return SipTimerEventImpl.TRYING;
            case A:
                return SipTimerEventImpl.A;
            case B:
                return SipTimerEventImpl.B;
            case C:
                return SipTimerEventImpl.C;
            case D:
                return SipTimerEventImpl.D;
            case E:
                return SipTimerEventImpl.E;
            case F:
                return SipTimerEventImpl.F;
            case G:
                return SipTimerEventImpl.G;
            case H:
                return SipTimerEventImpl.H;
            case I:
                return SipTimerEventImpl.I;
            case J:
                return SipTimerEventImpl.J;
            case K:
                return SipTimerEventImpl.K;
            case L:
                return SipTimerEventImpl.L;
            case M:
                return SipTimerEventImpl.M;
            case Timeout:
                return SipTimerEventImpl.TIMEOUT;
            case Timeout1:
                return SipTimerEventImpl.TIMEOUT1;
            case Timeout2:
                return SipTimerEventImpl.TIMEOUT2;
            case Timeout3:
                return SipTimerEventImpl.TIMEOUT3;
            case Timeout4:
                return SipTimerEventImpl.TIMEOUT4;
            default:
                throw new RuntimeException("Don't know which timer you are talking about.");
        }
    }
}
