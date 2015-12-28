package io.sipstack.transport.event;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipRequestFlowEvent extends SipFlowEvent {

    default boolean isSipRequestEvent() {
        return true;
    }

    default SipRequestFlowEvent toSipRequestFlowEvent() {
        return this;
    }
}
