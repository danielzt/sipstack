package io.sipstack.transport.event;

import io.sipstack.transport.Flow;
import io.sipstack.transport.event.impl.FlowTerminatedEventImpl;

/**
 * Event for indicating that a flow has been terminated and removed from the system.
 *
 * @author jonas@jonasborjesson.com
 */
public interface FlowTerminatedEvent extends FlowLifeCycleEvent {

    @Override
    default boolean isFlowTerminatedEvent() {
        return true;
    }

    static FlowTerminatedEvent create(final Flow flow) {
        return new FlowTerminatedEventImpl(flow);
    }
}
