package io.sipstack.netty.codec.sip.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

/**
 * @author jonas@jonasborjesson.com
 */
public class FlowConfiguration {
    @JsonProperty
    private Duration timeout = Duration.ofMinutes(3);

    public Duration timeout() {
        return timeout;
    }

}
