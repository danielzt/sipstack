package io.sipstack.transport.event;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipResponseFlowEvent extends SipFlowEvent {

    default SipResponseFlowEvent toSipResponseFlowEvent() {
        return this;
    }

    default boolean isSipResponseEvent() {
        return true;
    }
}
