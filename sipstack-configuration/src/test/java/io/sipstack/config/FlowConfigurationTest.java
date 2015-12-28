package io.sipstack.config;

import io.sipstack.config.KeepAliveMethodConfiguration.PING_METHOD;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import sun.security.krb5.Config;

import java.time.Duration;
import java.util.List;

import static io.sipstack.config.KeepAliveConfiguration.KEEP_ALIVE_MODE.NONE;
import static io.sipstack.config.KeepAliveConfiguration.KEEP_ALIVE_MODE.PASSIVE;
import static io.sipstack.config.KeepAliveMethodConfiguration.PING_METHOD.DOUBLE_CRLF;
import static io.sipstack.config.KeepAliveMethodConfiguration.PING_METHOD.SIP_OPTIONS;
import static io.sipstack.config.KeepAliveMethodConfiguration.PING_METHOD.STUN;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author jonas@jonasborjesson.com
 */
public class FlowConfigurationTest extends ConfigTestBase {

    @Test
    public void testKeepAliveConfiguration01() throws Exception {
        final SipConfiguration sip = loadConfiguration(SipConfiguration.class, "FlowConfigurationTest01.yaml");
        final FlowConfiguration flowConfig = sip.getTransport().getFlow();

        final KeepAliveConfiguration keepAliveConfig = flowConfig.getKeepAliveConfiguration();
        assertThat(keepAliveConfig.getMode(), is(NONE));
        assertThat(keepAliveConfig.getIdleTimeout().getSeconds(), is(40L));
        assertThat(keepAliveConfig.getInterval().getSeconds(), is(5L));
        assertThat(keepAliveConfig.getMaxFailed(), is(3));
        assertThat(keepAliveConfig.getEnforcePong(), is(true));

        final KeepAliveMethodConfiguration udpConfig = keepAliveConfig.getUdpKeepAliveMethodConfiguratioh();
        assertThat(udpConfig.getActiveMethod(), is(SIP_OPTIONS));
        assertThat(udpConfig.getAcceptedMethods().size(), is(2));
        assertPingMethod(udpConfig.getAcceptedMethods(), STUN);
        assertPingMethod(udpConfig.getAcceptedMethods(), SIP_OPTIONS);

        final SipOptionsPingConfiguration pingConfig = udpConfig.getSipOptionsConfiguration();


        final KeepAliveMethodConfiguration tcpConfig = keepAliveConfig.getTcpKeepAliveMethodConfiguratioh();
        assertThat(tcpConfig.getActiveMethod(), is(DOUBLE_CRLF));
        assertThat(tcpConfig.getAcceptedMethods().size(), is(2));
        assertPingMethod(tcpConfig.getAcceptedMethods(), DOUBLE_CRLF);
        assertPingMethod(tcpConfig.getAcceptedMethods(), SIP_OPTIONS);

        final KeepAliveMethodConfiguration wsConfig = keepAliveConfig.getWsKeepAliveMethodConfiguratioh();
        assertThat(wsConfig.getActiveMethod(), is(DOUBLE_CRLF));
        assertThat(wsConfig.getAcceptedMethods().size(), is(1));
        assertPingMethod(wsConfig.getAcceptedMethods(), SIP_OPTIONS);
    }

    private boolean assertPingMethod(final List<PING_METHOD> methods, PING_METHOD method) {
        return methods.stream().filter(m -> m == method).findFirst().isPresent();
    }

    @Test
    public void testKeepAliveConfiguration02() throws Exception {
        final SipConfiguration sip = loadConfiguration(SipConfiguration.class, "FlowConfigurationTest01b.yaml");
        final FlowConfiguration flowConfig = sip.getTransport().getFlow();

        final KeepAliveConfiguration keepAliveConfig = flowConfig.getKeepAliveConfiguration();
        assertThat(keepAliveConfig.getMode(), is(PASSIVE));
        assertThat(keepAliveConfig.getIdleTimeout().getSeconds(), is(123L));
        assertThat(keepAliveConfig.getInterval().getSeconds(), is(9L));
        assertThat(keepAliveConfig.getMaxFailed(), is(12));
        assertThat(keepAliveConfig.getEnforcePong(), is(false));
    }

    @Test
    public void testFlowConfiguration() throws Exception {
        final SipConfiguration sip = loadConfiguration(SipConfiguration.class, "FlowConfigurationTest02.yaml");
        final FlowConfiguration flowConfig = sip.getTransport().getFlow();
        assertThat(flowConfig.getInitialIdleTimeout(), is(Duration.ofSeconds(10)));

    }
}
