package io.sipstack.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * All configuration concerning the transport layer, such as flow control.
 *
 * @author jonas@jonasborjesson.com
 */
public class TransportLayerConfiguration {

    /**
     * If the rport is present on the top-most via we should fill it out
     * for the responses we send back.
     *
     * If it isn't, we shouldn't according to RFC, which is kind of silly
     * so by forcing the RPort we will always do it no matter what.
     */
    @JsonProperty
    private boolean forceRPort = true;

    /**
     * For requests, you really should push an rport parameter as a flag
     * parameter to indicate to the other element that you wish it to fill
     * it out. However, if you are building some obscure tool or you just
     * want a stack that is weird then you can turn it off.
     */
    @JsonProperty
    private boolean pushRport = true;

    @JsonProperty
    private FlowConfiguration flow = new FlowConfiguration();

    public FlowConfiguration getFlow() {
        return flow;
    }

    public boolean getForceRPort() {
        return forceRPort;
    }

    public boolean isPushRPort() {
        return pushRport;
    }

    public void setPushRPort(boolean value) {
        pushRport = value;
    }
}
