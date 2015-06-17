package io.sipstack.netty.codec.sip;

import org.junit.Test;

import java.time.Duration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author jonas@jonasborjesson.com
 */
public class UtilsTest {

    /**
     * See RFC3261 section 17.1.2.2
     *
     * Default values of t1 = 500 ms and t4 of 4000 ms will yield:
     *
     * 500 ms, 1 s, 2 s, 4 s, 4 s, 4 s
     */
    @Test
    public void testBackoff() {
        assertThat(Utils.calculateBackoffTimer(0, 500, 4000), is(Duration.ofMillis(500)));
        assertThat(Utils.calculateBackoffTimer(1, 500, 4000), is(Duration.ofMillis(1000)));
        assertThat(Utils.calculateBackoffTimer(2, 500, 4000), is(Duration.ofMillis(2000)));
        assertThat(Utils.calculateBackoffTimer(3, 500, 4000), is(Duration.ofMillis(4000)));
        assertThat(Utils.calculateBackoffTimer(4, 500, 4000), is(Duration.ofMillis(4000)));
        assertThat(Utils.calculateBackoffTimer(5, 500, 4000), is(Duration.ofMillis(4000)));
        assertThat(Utils.calculateBackoffTimer(6, 500, 4000), is(Duration.ofMillis(4000)));
    }

}