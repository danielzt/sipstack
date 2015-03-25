/**
 * 
 */
package io.sipstack.actor;

import io.sipstack.event.Event;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Actor {

    default void onEvent(final ActorContext ctx, final Event event) {
        ctx.forward(event);
        // ctx.next(event);
        // ctx.reverse().next(event);
        // ctx.reverse().fire(event);
    }

    default ActorRef self() {
        return null;
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
