/**
 * 
 */
package io.sipstack.actor;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public class ActorUtils {

    public static Optional<Throwable> safePreStart(final Actor actor) {
        if (actor == null) {
            return Optional.empty();
        }

        try {
            actor.preStart();
            return Optional.empty();
        } catch (final Throwable t) {
            return Optional.of(t);
        }
    }

    public static Optional<Throwable> safePostStop(final Actor actor) {
        if (actor == null) {
            return Optional.empty();
        }

        try {
            actor.postStop();
            return Optional.empty();
        } catch (final Throwable t) {
            return Optional.of(t);
        }
    }

}
