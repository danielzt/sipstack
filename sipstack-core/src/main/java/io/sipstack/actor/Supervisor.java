/**
 * 
 */
package io.sipstack.actor;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public interface Supervisor {

    void killChild(Actor actor);

}
