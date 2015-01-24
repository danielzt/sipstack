/**
 * 
 */
package io.sipstack.config;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The base class for all SIP related configuration options.
 *
 * @author jonas@jonasborjesson.com
 */
public class SipConfiguration {

    // @JsonProperty
    // private List<ListeningPointConfiguration> listen;

    @JsonProperty("interface")
    private List<NetworkInterfaceConfiguration> networkInterfaces;

    @JsonProperty()
    private TransactionLayerConfiguration transaction;

    @JsonIgnore
    public List<NetworkInterfaceConfiguration> getNetworkInterfaces() {
        if (this.networkInterfaces == null) {
            return Collections.emptyList();
        }
        return this.networkInterfaces;
    }

    /**
     * @return the transaction
     */
    public TransactionLayerConfiguration getTransaction() {
        return this.transaction;
    }

}
