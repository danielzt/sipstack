package io.sipstack.example.proxy.simple001;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Some basic configuration options for our very simple Proxy example app
 *
 * @author jonas@jonasborjesson.com
 */
public class SimpleProxyConfiguration {

    /**
     * The name of our stack. Just a silly example of a configuration option.
     */
    @JsonProperty
    private String name;

    @JsonProperty
    private String listenAddress = "127.0.0.1";

    @JsonProperty
    private int listenPort = 5060;

    public String getName() {
        return name;
    }

    public String getListenAddress() {
        return listenAddress;
    }

    public int getListenPort() {
        return listenPort;
    }

}
