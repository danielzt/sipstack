/**
 * 
 */
package io.sipstack.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.hektor.config.HektorConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Sipstack.io is configured through a YAML file and this class represents that
 * configuration. Please extend this class with your own configuration and they
 * will automatically be parsed from the YAML file as well.
 * 
 * @author jonas@jonasborjesson.com
 */
public class Configuration {

    /**
     * The name of the io.sipstack.application.application.
     */
    @Valid
    @NotNull
    @JsonProperty
    private String name;


    @Valid
    @NotNull
    @JsonProperty
    private SipConfiguration sip = new SipConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private HektorConfiguration hektor = new HektorConfiguration();

    /**
     * Set the SIP specific configuration.
     * 
     * @param config
     */
    public void setSipConfiguration(final SipConfiguration config) {
        sip = config;
    }

    /**
     * Get the SIP specific configuration.
     * 
     * @return
     */
    public SipConfiguration getSipConfiguration() {
        return sip;
    }

    public HektorConfiguration getHektorConfiguration() {
        return hektor;
    }

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
