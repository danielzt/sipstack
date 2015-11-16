package io.sipstack.transport.event;

/**
 * @author jonas@jonasborjesson.com
 */
public interface FlowLifeCycleEvent extends FlowEvent {

    @Override
    default boolean isFlowLifeCycleEvent() {
        return true;
    }

    @Override
    default FlowLifeCycleEvent toFlowLifeCycleEvent() {
        return this;
    }
}
