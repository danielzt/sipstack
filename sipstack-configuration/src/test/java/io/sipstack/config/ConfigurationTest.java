package io.sipstack.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import io.pkts.packet.sip.address.SipURI;

import org.junit.Test;

public class ConfigurationTest extends ConfigTestBase {


    /**
     * Ensure that we can load configuration with our own configuration file.
     * 
     * @throws Exception
     */
    @Test
    public void testLoadBasicConfiguration() throws Exception {
        final SipConfiguration sip = loadConfiguration(SipConfiguration.class, "UnitTest.yaml");

        assertThat(sip.getNetworkInterfaces().size(), is(1));

        final NetworkInterfaceConfiguration network = sip.getNetworkInterfaces().get(0);

        assertThat(network.getListeningAddress().getPort(), is(5060));
        assertThat(network.getListeningAddress().getHost().toString(), is("127.0.0.1"));

        assertThat(network.getVipAddress().getPort(), is(5060));
        assertThat(network.getVipAddress().getHost().toString(), is("64.92.13.45"));

        assertThat(network.hasTCP(), is(true));
        assertThat(network.hasUDP(), is(true));
        assertThat(network.hasTLS(), is(false));
        assertThat(network.hasWS(), is(false));
        assertThat(network.hasSCTP(), is(false));
    }

    @Test
    public void testLoadConfiguration() throws Exception {
        final SipConfiguration sip = loadConfiguration(SipConfiguration.class, "UnitTestManyNetworkInterfaces.yaml");

        assertThat(sip.getNetworkInterfaces().size(), is(4));

        NetworkInterfaceConfiguration network = sip.getNetworkInterfaces().get(0);
        assertThat(network.getName(), is("public"));
        assertThat(network.getListeningAddress().getPort(), is(5090));
        assertThat(network.getListeningAddress().getHost().toString(), is("62.10.20.40"));
        assertThat(network.getVipAddress().getPort(), is(5060));
        assertThat(network.getVipAddress().getHost().toString(), is("64.92.13.45"));
        assertThat(network.hasTCP(), is(false));
        assertThat(network.hasUDP(), is(false));
        assertThat(network.hasTLS(), is(true));
        assertThat(network.hasWS(), is(false));
        assertThat(network.hasSCTP(), is(false));

        network = sip.getNetworkInterfaces().get(1);
        assertThat(network.getName(), is("private"));
        assertThat(network.getListeningAddress().getPort(), is(-1));
        assertThat(network.getListeningAddress().getHost().toString(), is("10.36.10.100"));
        assertThat(network.getVipAddress(), is((SipURI)null));
        assertThat(network.hasTCP(), is(false));
        assertThat(network.hasUDP(), is(true));
        assertThat(network.hasTLS(), is(false));
        assertThat(network.hasWS(), is(false));
        assertThat(network.hasSCTP(), is(false));

        network = sip.getNetworkInterfaces().get(2);
        assertThat(network.getName(), is("local"));
        assertThat(network.getListeningAddress().getPort(), is(-1));
        assertThat(network.getListeningAddress().getHost().toString(), is("127.0.0.1"));
        assertThat(network.getVipAddress(), is((SipURI)null));
        assertThat(network.hasTCP(), is(true));
        assertThat(network.hasUDP(), is(true));
        assertThat(network.hasTLS(), is(false));
        assertThat(network.hasWS(), is(false));
        assertThat(network.hasSCTP(), is(false));

        network = sip.getNetworkInterfaces().get(3);
        assertThat(network.getName(), is("wlan0"));
        assertThat(network.getListeningAddress().getPort(), is(-1));
        assertThat(network.getListeningAddress().getHost().toString(), is("192.168.0.100"));
        assertThat(network.getVipAddress(), is((SipURI)null));
        assertThat(network.hasTCP(), is(false));
        assertThat(network.hasUDP(), is(false));
        assertThat(network.hasTLS(), is(false));
        assertThat(network.hasWS(), is(true));
        assertThat(network.hasSCTP(), is(false));
    }

}
