package io.sipstack.config;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import sun.security.krb5.Config;

import java.time.Duration;

import static io.sipstack.config.KeepAliveConfiguration.KEEP_ALIVE_MODE.NONE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author jonas@jonasborjesson.com
 */
public class FlowConfigurationTest extends ConfigTestBase {

    @Test
    public void testKeepAliveConfiguration() throws Exception {
        final SipConfiguration sip = loadConfiguration(SipConfiguration.class, "FlowConfigurationTest01.yaml");
        final FlowConfiguration flowConfig = sip.getTransport().getFlow();

        final KeepAliveConfiguration keepAliveConfig = flowConfig.getKeepAliveConfiguration();
        assertThat(keepAliveConfig.getMode(), is(NONE));
    }

    @Test
    public void testFlowConfiguration() throws Exception {
        final SipConfiguration sip = loadConfiguration(SipConfiguration.class, "FlowConfigurationTest02.yaml");
        final FlowConfiguration flowConfig = sip.getTransport().getFlow();
        assertThat(flowConfig.getInitialIdleTimeout(), is(Duration.ofSeconds(10)));

    }
}
