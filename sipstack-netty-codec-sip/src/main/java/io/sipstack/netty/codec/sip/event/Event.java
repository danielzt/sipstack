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

}
