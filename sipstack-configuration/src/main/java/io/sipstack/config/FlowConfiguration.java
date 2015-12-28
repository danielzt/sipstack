package io.sipstack.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

import static io.sipstack.config.KeepAliveConfiguration.KEEP_ALIVE_MODE.ACTIVE;
import static io.sipstack.config.KeepAliveConfiguration.KEEP_ALIVE_MODE.NONE;
import static io.sipstack.config.KeepAliveConfiguration.KEEP_ALIVE_MODE.PASSIVE;

/**
 * @author jonas@jonasborjesson.com
 */
public class FlowConfiguration {

    @JsonProperty
    private Duration timeout = Duration.ofMinutes(3);

    @JsonProperty
    private Duration initialIdleTimeout = Duration.ofSeconds(4);

    @JsonProperty
    private int defaultStorageSize = 1000;

    @JsonProperty("keepAlive")
    private KeepAliveConfiguration keepAliveConfiguration = new KeepAliveConfiguration();

    /**
     *
     * @return
     */
    public Duration getTimeout() {
        return timeout;
    }

    public KeepAliveConfiguration getKeepAliveConfiguration() {
        return keepAliveConfiguration;
    }

    /**
     * Convenience method for checking if the keep-alive (ping) mode is set to
     * active.
     *
     * @return
     */
    public boolean isPingModeActive() {
        return keepAliveConfiguration.getMode() == ACTIVE;
    }

    /**
     * Convenience method for checking if the keep-alive (ping) mode is set to
     * passive.
     *
     * @return
     */
    public boolean isPingModePassive() {
        return keepAliveConfiguration.getMode() == PASSIVE;
    }

    /**
     * Convenience method for checking if the keep-alive (ping) mode is off.
     *
     * @return
     */
    public boolean isPingModeOff() {
        return keepAliveConfiguration.getMode() == NONE;
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

    /**
     * The size of the internal flow storage. Default is 1000 but you should
     * adjust this to the expected maximum number of open flows at any given
     * point. The internal storage will automatically grow, but when it does
     * so you will very likely incur a steep penalty so it is better to
     * allocate the correct size upfront (default implementation is a map
     * and we all know what happens with a map once it has to resize!)
     */
    public int getDefaultStorageSize() {
        return defaultStorageSize;
    }

    public void setDefaultStorageSize(final int size) {
        defaultStorageSize = size;
    }

}
