package io.sipstack.transport.event.impl;

import io.sipstack.transport.Flow;
import io.sipstack.transport.event.FlowEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class FlowEventImpl implements FlowEvent {

    private final Flow flow;

    public FlowEventImpl(final Flow flow) {
        this.flow = flow;
    }

    @Override
    public Flow flow() {
        return flow;
    }
}
