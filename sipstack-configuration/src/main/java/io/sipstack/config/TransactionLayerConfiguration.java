/**
 * 
 */
package io.sipstack.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author jonas@jonasborjesson.com
 */
public final class TransactionLayerConfiguration {
    
    /**
     * SIP specifications says that the Invite Server Transaction
     * should send 100 Trying if the TU won't within 200ms, however,
     * quite often  you may just want the io.sipstack.transaction.transaction to do it right
     * away. In sipstack.io, it is the default behavior to send the 100 Trying
     * right away but can be changed by setting this property to false.
     */
    @JsonProperty
    private boolean send100TryingImmediately = true;

    /**
     * Configuration object for all Timers as defined within the various SIP specifications.
     */
    @JsonProperty
    private TimersConfiguration timers = new TimersConfiguration();

    /**
     * The size of the internal transaction storage. Default is 500k
     */
    @JsonProperty
    private int defaultStorageSize = 500000;

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

    /**
     * @return the send100TryingImmediately
     */
    public boolean isSend100TryingImmediately() {
        return this.send100TryingImmediately;
    }
    
    public void setSend100TryingImmediately(final boolean value) {
        this.send100TryingImmediately = value;
    }

    public int getDefaultStorageSize() {
        return defaultStorageSize;
    }

    public void setDefaultStorageSize(int defaultStorageSize) {
        this.defaultStorageSize = defaultStorageSize;
    }
}
