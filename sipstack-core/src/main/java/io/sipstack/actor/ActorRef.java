/**
 * 
 */
package io.sipstack.actor;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ActorRef {

    /**
     * The worker pool this actor belongs to.
     * 
     * @return
     */
    int workerPool();

}
