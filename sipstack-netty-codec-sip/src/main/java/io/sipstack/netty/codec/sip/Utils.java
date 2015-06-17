package io.sipstack.netty.codec.sip;

import java.time.Duration;

/**
 * Can't have a library without Utils class!
 *
 * @author jonas@jonasborjesson.com
 */
public class Utils {

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
