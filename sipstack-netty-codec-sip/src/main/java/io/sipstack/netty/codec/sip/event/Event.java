package io.sipstack.netty.codec.sip.event;

/**
 * @author jonas@jonasborjesson.com
 */
public abstract class Event {

    private final long arrivalTime;

    public Event(final long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public final long arrivalTime() {
        return arrivalTime;
    }

    /**
     * Check whether this event is actually a {@link io.pkts.packet.sip.SipMessage}.
     */
    public boolean isSipMessageEvent() {
        return false;
    }

    public SipMessageEvent toSipMessageEvent() {
        throw new ClassCastException("Cannot case " + getClass().getName() + " into a " + SipMessageEvent.class.getName());
    }

    public boolean isSipTimerEvent() {
        return false;
    }

    /**
     * There isn't an official "100 Trying" timer but if you look in the state machine for an Invite
     * Server Transaction it states that it should send a 100 Trying after 200 ms unless the TU does
     * so itself. Hence, this is the timer that keeps track of that.
     *
     * @return
     */
    public boolean isSipTimer100Trying() {
        return false;
    }

    public boolean isSipTimerA() {
        return false;
    }

    public boolean isSipTimerB() {
        return false;
    }

    public boolean isSipTimerC() {
        return false;
    }

    public boolean isSipTimerD() {
        return false;
    }

    public boolean isSipTimerE() {
        return false;
    }

    public boolean isSipTimerF() {
        return false;
    }

    public boolean isSipTimerG() {
        return false;
    }

    public boolean isSipTimerH() {
        return false;
    }

    public boolean isSipTimerI() {
        return false;
    }

    public boolean isSipTimerJ() {
        return false;
    }

    public boolean isSipTimerK() {
        return false;
    }

    public boolean isSipTimerL() {
        return false;
    }

    public boolean isSipTimerM() {
        return false;
    }
}
