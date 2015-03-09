/**
 * 
 */
package io.sipstack.actor;

import io.sipstack.event.Event;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Actor {

    void onUpstreamEvent(ActorContext ctx, Event event);

    void onDownstreamEvent(ActorContext ctx, Event event);

    default Supervisor getSupervisor() {
        return null;
    }
}
