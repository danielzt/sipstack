package io.sipstack.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ListeningPointConfigurationTest extends ConfigTestBase {

    @Before public void setUp() throws Exception { 
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testLoadConfiguration() throws Exception {
        final SipConfiguration config = loadConfiguration(SipConfiguration.class, "sipstack.yaml");

        // assertThat(config.getListeningPoints().size(), is(3));

        // ListeningPointConfiguration lp = config.getListeningPoints().get(0);
        // assertThat(lp.getIp().toString(), is("127.0.0.1"));
        // assertThat(lp.getPort(), is(5060));
        // assertThat(lp.getTransport().toString(), is("tcp"));
        // assertThat(lp.getVipIP().toString(), is("64.92.13.45"));
        // assertThat(lp.getVipPort(), is(5090));

        // lp = config.getListeningPoints().get(1);
        // assertThat(lp.getIp().toString(), is("127.0.0.1"));
        // assertThat(lp.getPort(), is(-1));
        // assertThat(lp.getTransport().toString(), is("udp"));
        // assertThat(lp.getVipIP().toString(), is("64.92.13.45"));
        // assertThat(lp.getVipPort(), is(-1));
    }

    @Test
    public void testParseListeningPoint() throws Exception {
        assertListeningPoint("127.0.0.1", 5060, "tcp");
        assertListeningPoint("127.0.0.1", 5060, "udp");
        assertListeningPoint("62.168.0.100", -1, "udp");
        assertListeningPoint("62.168.0.100", 9999, "ws");
        assertListeningPoint("62.168.0.100", 9999, null);

        assertListeningPoint("127.0.0.1", 5060, "udp", "64.64.64.64", -1);
        assertListeningPoint("127.0.0.1", 5060, "udp", "64.64.64.64", 7070);

    }

    private void assertListeningPoint(final String host, final int port, final String transport, final String asHost, final int asPort) {
        String value = host 
                + (port > -1 ? ":" + port : "") 
                + (transport != null ? ";transport=" + transport : "");

        if (asHost != null) {
            value += " as " + asHost
                    + (asPort > -1 ? ":" + asPort : "");
        }

        final ListeningPointConfiguration lp = ListeningPointConfiguration.create(value);

        assertThat(lp.getIp().toString(), is(host));
        if (port > -1) {
            assertThat(lp.getPort(), is(port));
        } else {
            assertThat(lp.getPort(), is(-1));
        }

        if (transport != null) {
            assertThat(lp.getTransport().toString(), is(transport));
        } else {
            assertThat(lp.getTransport().toString(), is(""));
        }

        if (asHost != null) {
            assertThat(lp.getVipIP().toString(), is(asHost));
        } else {
            assertThat(lp.getVipIP().toString(), is(""));
        }

        if (asPort < 0) {
            assertThat(lp.getVipPort(), is(-1));
        } else {
            assertThat(lp.getVipPort(), is(asPort));
        }

    }

    private void assertListeningPoint(final String host, final int port, final String transport) {
        assertListeningPoint(host, port, transport, null, -1);
    }

}