/**
 * 
 */
package io.sipstack.actor;

import io.sipstack.event.Event;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Actor {

    default void onUpstreamEvent(final ActorContext ctx, final Event event) {
        ctx.forwardUpstreamEvent(event);
    }

    default void onDownstreamEvent(final ActorContext ctx, final Event event) {
        ctx.forwardDownstreamEvent(event);
    }

    default Supervisor getSupervisor() {
        return null;
    }
}
