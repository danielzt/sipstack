package io.sipstack.transport.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.transport.Flow;
import io.sipstack.transport.event.impl.SipRequestBuilderFlowEventImpl;
import io.sipstack.transport.event.impl.SipRequestFlowEventImpl;
import io.sipstack.transport.event.impl.SipResponseBuilderFlowEventImpl;
import io.sipstack.transport.event.impl.SipResponseFlowEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface FlowEvent {

    Flow flow();

    // =====================================
    // === SIP flow builder events
    // =====================================

    default boolean isSipBuilderFlowEvent() {
        return false;
    }

    default SipBuilderFlowEvent toSipBuilderFlowEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipFlowEvent.class.getName());
    }


    /*
    static SipBuilderFlowEvent create(final Flow flow, final SipMessage.Builder<? extends SipMessage> builder) {
        if (builder.isSipRequestBuilder()) {
            return create(flow, builder.toSipRequestBuilder());
        }
        return create(flow, builder.toSipResponseBuilder());
    }
    */

    // =====================================
    // === SIP request builder flow events
    // =====================================
    default boolean isSipRequestBuilderFlowEvent() {
        return false;
    }

    default SipRequestBuilderFlowEvent toSipRequestBuilderFlowEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipRequestBuilderFlowEvent.class.getName());
    }


    static SipRequestBuilderFlowEvent create(final Flow flow, final SipMessage.Builder<SipRequest> builder) {
        return new SipRequestBuilderFlowEventImpl(flow, builder);
    }

    // =====================================
    // === SIP response builder flow events
    // =====================================
    default boolean isSipResponseBuilderFlowEvent() {
        return false;
    }

    default SipResponseBuilderFlowEvent toSipResponseBuilderFlowEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipResponseBuilderFlowEvent.class.getName());
    }


    static SipResponseBuilderFlowEvent create(final Flow flow, final SipMessage.Builder<SipResponse> builder) {
        return new SipResponseBuilderFlowEventImpl(flow, builder);
    }

    // =====================================
    // === SIP flow events
    // =====================================

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

    static SipFlowEvent create(final Flow flow, final SipMessage message) {
        if (message.isRequest()) {
            return create(flow, message.toRequest());
        }
        return create(flow, message.toResponse());
    }

    // =====================================
    // === SIP request flow events
    // =====================================
    default boolean isSipRequestFlowEvent() {
        return false;
    }

    default SipRequestFlowEvent toSipRequestFlowEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipRequestFlowEvent.class.getName());
    }


    static SipRequestFlowEvent create(final Flow flow, final SipRequest request) {
        return new SipRequestFlowEventImpl(flow, request);
    }

    // =====================================
    // === SIP response flow events
    // =====================================
    default boolean isSipResponseFlowEvent() {
        return false;
    }

    default SipResponseFlowEvent toSipResponseFlowEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipResponseFlowEvent.class.getName());
    }


    static SipResponseFlowEvent create(final Flow flow, final SipResponse response) {
        return new SipResponseFlowEventImpl(flow, response);
    }

    // =====================================
    // === Life-cycle events
    // =====================================
    default boolean isFlowLifeCycleEvent() {
        return false;
    }

    default FlowLifeCycleEvent toFlowLifeCycleEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + FlowLifeCycleEvent.class.getName());
    }

    default boolean isFlowTerminatedEvent() {
        return false;
    }

}
