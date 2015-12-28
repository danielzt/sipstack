package io.sipstack;

import io.sipstack.netty.codec.sip.Clock;

import java.time.Duration;

/**
 * Implementation of the {@link Clock} interface and is essentially just
 * a wrapper around a {@link Duration} and allows you to "manipulate" time
 * which is great for your unit tests.
 *
 * @author jonas@jonasborjesson.com
 */
public class ControllableClock implements Clock {

    private Duration currentTime;

    public ControllableClock() {
        this.currentTime = Duration.ofMillis(System.currentTimeMillis());
    }

    public void tick(final Duration duration) {

    }

    public void plusMillis(final long millisToAdd) {
        currentTime = currentTime.plusMillis(millisToAdd);
    }

    public void plusSeconds(final long secondsToAdd) {
        currentTime = currentTime.plusSeconds(secondsToAdd);
    }

    @Override
    public long getCurrentTimeMillis() {
        return currentTime.toMillis();
    }
}
