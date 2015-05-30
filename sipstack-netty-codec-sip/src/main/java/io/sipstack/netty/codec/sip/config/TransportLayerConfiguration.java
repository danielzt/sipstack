package io.sipstack.netty.codec.sip.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * All configuration concerning the transport layer, such as flow control.
 *
 * @author jonas@jonasborjesson.com
 */
public class TransportLayerConfiguration {

    @JsonProperty
    private FlowConfiguration flow = new FlowConfiguration();

    public FlowConfiguration getFlow() {
        return flow;
    }
}
