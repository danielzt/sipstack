/**
 * 
 */
package io.sipstack.actor;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Actor {

    void onUpstreamEvent(ActorContext ctx, Event event);

    void onDownstreamEvent(ActorContext ctx, Event event);

}
