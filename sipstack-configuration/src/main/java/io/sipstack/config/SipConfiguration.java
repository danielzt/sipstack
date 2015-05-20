/**
 * 
 */
package io.sipstack.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.pkts.packet.sip.impl.PreConditions;

import java.util.Collections;
import java.util.List;

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

    @JsonProperty
    private final TransactionLayerConfiguration transaction = new TransactionLayerConfiguration();

    @JsonProperty
    private final TransportLayerConfiguration transport = new TransportLayerConfiguration();

    @JsonProperty
    private int workerThreads = 4;

    @JsonIgnore
    public List<NetworkInterfaceConfiguration> getNetworkInterfaces() {
        if (networkInterfaces == null) {
            return Collections.emptyList();
        }
        return networkInterfaces;
    }

    /**
     * @return the transaction
     */
    public TransactionLayerConfiguration getTransaction() {
        return transaction;
    }

    public TransportLayerConfiguration getTransport() {
        return transport;
    }

    /**
     * @return the workerThreads
     */
    public int getWorkerThreads() {
        return workerThreads;
    }

    /**
     * @param workerThreads the workerThreads to set
     */
    public void setWorkerThreads(final int workerThreads) {
        PreConditions.ensureArgument(workerThreads > 0, "The number of worker threads must be greater than zero");
        this.workerThreads = workerThreads;
    }

}
