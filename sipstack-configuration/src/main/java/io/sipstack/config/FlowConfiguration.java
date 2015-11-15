package io.sipstack.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

/**
 * @author jonas@jonasborjesson.com
 */
public class FlowConfiguration {

    @JsonProperty
    private Duration timeout = Duration.ofMinutes(3);

    @JsonProperty
    private Duration initialIdleTimeout = Duration.ofSeconds(4);

    /**
     * The size of the internal flow storage. Default is 1000 but you should
     * adjust this to the expected maximum number of open flows at any given
     * point. The internal storage will automatically grow, but when it does
     * so you will very likely incur a steep penalty so it is better to
     * allocate the correct size upfront (default implementation is a map
     * and we all know what happens with a map once it has to resize!)
     */
    @JsonProperty
    private int defaultStorageSize = 1000;

    public Duration getTimeout() {
        return timeout;
    }

    /**
     * When a flow accepts a new connection and enters the ready state it will
     * wait a maximum number of seconds for anything to be sent across that flow.
     * If nothing is showing up then the flow will be closed.
     *
     * This is typically only useful for flows based off of TCP connections
     * where you want to prevent attacks where someone just establish a TCP
     * connection but then never actually send traffic across that connection.
     *
     * @return
     */
    public Duration getInitialIdleTimeout() {
        return initialIdleTimeout;
    }

    public int getDefaultStorageSize() {
        return defaultStorageSize;
    }

    public void setDefaultStorageSize(final int size) {
        defaultStorageSize = size;
    }

}
