/**
 * 
 */
package io.sipstack.actor;

/**
 * @author jonas
 *
 */
public interface Actor {

    void onEvent(ActorContext ctx, Event event);

    void onDownstreamEvent(ActorContext ctx, Event event);

}
