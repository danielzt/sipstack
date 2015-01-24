/**
 * 
 */
package io.sipstack.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author jonas
 *
 */
public final class TransactionLayerConfiguration {

    @JsonProperty
    private TimersConfiguration timers = new TimersConfiguration();

    /**
     * @return the timers
     */
    public TimersConfiguration getTimers() {
        return this.timers;
    }

    /**
     * @param timers the timers to set
     */
    public void setTimers(final TimersConfiguration timers) {
        this.timers = timers;
    }

}
