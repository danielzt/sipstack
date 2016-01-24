package io.sipstack.transport.event.impl;

import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.transport.Flow;
import io.sipstack.transport.event.FlowTerminatedEvent;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public class FlowTerminatedEventImpl extends FlowEventImpl implements FlowTerminatedEvent {

    public FlowTerminatedEventImpl(final Flow flow) {
        super(flow);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        try {
            final FlowTerminatedEvent other = (FlowTerminatedEvent)obj;
            final ConnectionId id = flow().id();
            final ConnectionId idOther = other.flow().id();
            return id.equals(idOther);
        } catch (final ClassCastException e) {
            return false;
        }
    }
}
