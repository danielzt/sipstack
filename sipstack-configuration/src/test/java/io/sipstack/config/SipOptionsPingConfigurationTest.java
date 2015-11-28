package io.sipstack.config;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author jonas@jonasborjesson.com
 */
public class SipOptionsPingConfigurationTest {

    /**
     * Just a test to ensure that you are made aware of the fact that by changing
     * the default values you MUST also update documentation.
     */
    @Test
    public void testDefaultValues() {
        final SipOptionsPingConfiguration config = new SipOptionsPingConfiguration();
        final String msg = "You have changed the default value, please update documentation as well!";
        assertThat(msg, config.getFromUser(), is("ping"));
        assertThat(msg, config.getToUser(), is("ping"));
        assertThat(msg, config.getTargetUser(), is("ping"));
    }
}
