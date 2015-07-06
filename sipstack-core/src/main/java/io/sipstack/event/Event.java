package io.sipstack.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;

/**
 * @author jonas@jonasborjesson.com
 */
public abstract class Event {

    /**
     * Check whether this event is actually a {@link io.pkts.packet.sip.SipRequest}.
     */
    public boolean isSipRequestEvent() {
        return false;
    }

    /**
     * Check whether this event is actually a {@link io.pkts.packet.sip.SipResponse}.
     */
    public boolean isSipResponseEvent() {
        return false;
    }

    public SipRequestEvent toSipRequestEvent() {
        throw new ClassCastException("Cannot case " + getClass().getName() + " into a " + SipRequestEvent.class.getName());
    }

    public SipResponseEvent toSipResponseEvent() {
        throw new ClassCastException("Cannot case " + getClass().getName() + " into a " + SipResponseEvent.class.getName());
    }

    public static final Event create(final SipRequest request) {
        return new SipRequestEvent(request);
    }

    public static final Event create(final SipResponse response) {
        return new SipResponseEvent(response);
    }

    public static final Event create(final SipMessage msg) {
        if (msg.isRequest()) {
            return create(msg.toRequest());
        }

        return create(msg.toResponse());
    }

    public boolean isSipEvent() {
        return false;
    }

    /**
     * Convenience method for converting the event to a {@link SipResponseEvent} and
     * then grab the response from it.
     *
     * @return
     */
    public SipResponse response() {
        throw new ClassCastException("Cannot case " + getClass().getName() + " into a " + SipResponseEvent.class.getName());
    }

    /**
     * Convenience method for converting the event to a {@link SipRequestEvent} and
     * then grab the request from it.
     *
     * @return
     */
    public SipRequest request() {
        throw new ClassCastException("Cannot case " + getClass().getName() + " into a " + SipRequestEvent.class.getName());
    }

    public boolean isSipTimerEvent() {
        return false;
    }

    public SipTimerEvent toSipTimerEvent() {
        throw new ClassCastException("Cannot case " + getClass().getName() + " into a " + SipTimerEvent.class.getName());
    }

    @Override
    public boolean equals(final Object other) {
        return this == other;
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
