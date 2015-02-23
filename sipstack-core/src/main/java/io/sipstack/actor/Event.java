/**
 * 
 */
package io.sipstack.actor;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public interface Event {

    Key key();

    default boolean isSipMessage() {
        return false;
    }

}
