/**
 * 
 */
package io.sipstack.actor;

import java.time.Duration;
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


    /**
     * Several backoff timers within SIP is based on the below little algorithm. See 17.1.2.2 in
     * RFC3261.
     * 
     * @param count
     * @param baseTime
     * @param maxTime
     * @return
     */
    public static Duration calculateBackoffTimer(final int count, final long baseTime, final long maxTime) {
        return Duration.ofMillis(Math.min(baseTime * (int) Math.pow(2, count), maxTime));
    }

}
