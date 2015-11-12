package io.sipstack.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

/**
 * @author jonas@jonasborjesson.com
 */
public class FlowConfiguration {

    @JsonProperty
    private Duration timeout = Duration.ofMinutes(3);

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

    public int getDefaultStorageSize() {
        return defaultStorageSize;
    }

    public void setDefaultStorageSize(final int size) {
        defaultStorageSize = size;
    }

}
