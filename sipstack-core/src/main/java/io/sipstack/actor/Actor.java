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

    /**
     * Called exactly once when the {@link Actor} is created the very first time.
     */
    default void preStart() {
        // left empty intentionally. Actor implementations should
        // override and do something useful here
    }

    /**
     * Called exactly once when the {@link Actor} is stopped.
     */
    default void postStop() {
        // left empty intentionally. Actor implementations should
        // override and do something useful here
    }

    default Supervisor getSupervisor() {
        return null;
    }
}
