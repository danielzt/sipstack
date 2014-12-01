/**
 * 
 */
package io.sipstack.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sipstack.io is configured through a YAML file and this class represents that
 * configuration. Please extend this class with your own configuration and they
 * will automatically be parsed from the YAML file as well.
 * 
 * @author jonas@jonasborjesson.com
 */
public class Configuration {

    @Valid
    @NotNull
    @JsonProperty
    private SipConfiguration sip = new SipConfiguration();


    /**
     * Set the SIP specific configuration.
     * 
     * @param config
     */
    public void setSipConfiguration(final SipConfiguration config) {
        this.sip = config;
    }

    /**
     * Get the SIP specific configuration.
     * 
     * @return
     */
    public SipConfiguration getSipConfiguration() {
        return this.sip;
    }

}
