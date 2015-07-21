package io.sipstack.transport.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.transport.Flow;
import io.sipstack.transport.event.impl.SipRequestFlowEventImpl;
import io.sipstack.transport.event.impl.SipResponseFlowEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface FlowEvent {

    Flow flow();

    /**
     * Check if this is a Sip Event, which is either a request or response message.
     *
     * @return
     */
    default boolean isSipFlowEvent() {
        return false;
    }

    default SipFlowEvent toSipFlowEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipFlowEvent.class.getName());
    }

    default boolean isSipRequestFlowEvent() {
        return false;
    }

    default SipRequestFlowEvent toSipRequestFlowEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipRequestFlowEvent.class.getName());
    }

    default boolean isSipResponseFlowEvent() {
        return false;
    }

    default SipResponseFlowEvent toSipResponseFlowEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipResponseFlowEvent.class.getName());
    }

    static SipFlowEvent create(final Flow flow, final SipMessage message) {
        if (message.isRequest()) {
            return create(flow, message.toRequest());
        }
        return create(flow, message.toResponse());
    }

    static SipRequestFlowEvent create(final Flow flow, final SipRequest request) {
        return new SipRequestFlowEventImpl(flow, request);
    }

    static SipResponseFlowEvent create(final Flow flow, final SipResponse response) {
        return new SipResponseFlowEventImpl(flow, response);
    }

}
