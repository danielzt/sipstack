package io.sipstack.example.proxy.simple004;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.sipstack.config.NetworkInterfaceConfiguration;

import java.util.Collections;
import java.util.List;

/**
 * Some basic configuration options for our very simple Proxy example app
 *
 * @author jonas@jonasborjesson.com
 */
public class SimpleProxyConfiguration {

    @JsonProperty
    private List<NetworkInterfaceConfiguration> networkInterfaces;

    /**
     * The name of our stack. Just a silly example of a configuration option.
     */
    @JsonProperty
    private String name;

    public String getName() {
        return name;
    }

    @JsonIgnore
    public List<NetworkInterfaceConfiguration> getNetworkInterfaces() {
        if (networkInterfaces == null) {
            return Collections.emptyList();
        }
        return networkInterfaces;
    }

}
