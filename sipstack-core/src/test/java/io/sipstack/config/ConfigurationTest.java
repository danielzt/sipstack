package io.sipstack.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigurationTest extends ConfigTestBase {


    /**
     * Ensure that we can load configuration with our own configuration file.
     * 
     * @throws Exception
     */
    @Test
    public void testLoadConfiguration() throws Exception {
        final UnitTestConfig config = loadConfiguration(UnitTestConfig.class, "UnitTest.yaml");
        assertThat(config.getName(), is("hello"));
        final SipConfiguration sip = config.getSipConfiguration();
        assertThat(sip.getListeningPoints().size(), is(1));

        final ListeningPointConfiguration lp = sip.getListeningPoints().get(0);
        assertThat(lp.getIp().toString(), is("127.0.0.1"));
        assertThat(lp.getPort(), is(5060));
        assertThat(lp.getVipIP().toString(), is("192.168.0.100"));
    }

    public static class UnitTestConfig extends Configuration {

        @JsonProperty
        private String name;

        /**
         * @return the name
         */
        public String getName() {
            return this.name;
        }

        /**
         * @param name the name to set
         */
        public void setName(final String name) {
            this.name = name;
        }

    }

}
