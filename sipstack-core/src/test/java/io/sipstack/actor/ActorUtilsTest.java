/**
 * 
 */
package io.sipstack.actor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.Duration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jonas
 *
 */
public class ActorUtilsTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {}

    /**
     * See RFC3261 section 17.1.2.2
     * 
     * Default values of t1 = 500 ms and t4 of 4000 ms will yield:
     * 
     * 500 ms, 1 s, 2 s, 4 s, 4 s, 4 s
     */
    @Test
    public void testBackoff() {
        assertThat(ActorUtils.calculateBackoffTimer(0, 500, 4000), is(Duration.ofMillis(500)));
        assertThat(ActorUtils.calculateBackoffTimer(1, 500, 4000), is(Duration.ofMillis(1000)));
        assertThat(ActorUtils.calculateBackoffTimer(2, 500, 4000), is(Duration.ofMillis(2000)));
        assertThat(ActorUtils.calculateBackoffTimer(3, 500, 4000), is(Duration.ofMillis(4000)));
        assertThat(ActorUtils.calculateBackoffTimer(4, 500, 4000), is(Duration.ofMillis(4000)));
        assertThat(ActorUtils.calculateBackoffTimer(5, 500, 4000), is(Duration.ofMillis(4000)));
        assertThat(ActorUtils.calculateBackoffTimer(6, 500, 4000), is(Duration.ofMillis(4000)));
    }

}
