/**
 * 
 */
package io.sipstack.config;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author jonas@jonasborjesson.com
 */
public final class TimersConfiguration {

    @JsonProperty
    private Duration t1 = Duration.ofMillis(500);

    @JsonProperty
    private Duration t2 = Duration.ofSeconds(2);

    @JsonProperty
    private Duration t4 = Duration.ofSeconds(4);

    /**
     * @return the t1
     */
    public Duration getT1() {
        return this.t1;
    }

    /**
     * @return the t2
     */
    public Duration getT2() {
        return this.t2;
    }

    /**
     * @return the t4
     */
    public Duration getT4() {
        return this.t4;
    }

    /**
     * @param t1 the t1 to set
     */
    public void setT1(final Duration t1) {
        this.t1 = t1;
    }

    /**
     * @param t2 the t2 to set
     */
    public void setT2(final Duration t2) {
        this.t2 = t2;
    }

    /**
     * @param t4 the t4 to set
     */
    public void setT4(final Duration t4) {
        this.t4 = t4;
    }

}
