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
}
