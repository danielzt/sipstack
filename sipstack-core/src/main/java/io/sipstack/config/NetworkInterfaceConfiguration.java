/**
 * 
 */
package io.sipstack.config;

import io.pkts.packet.sip.address.SipURI;
import io.sipstack.core.Transport;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public class NetworkInterfaceConfiguration {

    @JsonProperty
    private final String name;

    /**
     * The IP-address that we will bind to.
     */
    @JsonProperty
    private final SipURI listen;

    @JsonProperty
    private final SipURI vipAddress;

    private final List<Transport> transports;

    /**
     * 
     */
    public NetworkInterfaceConfiguration(final String name, final SipURI listen, final SipURI vipAddress, final List<Transport> transports) {
        this.name = name;
        this.listen = listen;
        this.vipAddress = vipAddress;
        this.transports = transports;
    }

    @JsonIgnore
    public boolean hasUDP() {
        return this.transports.contains(Transport.udp);
    }

    @JsonIgnore
    public boolean hasTCP() {
        return this.transports.contains(Transport.tcp);
    }

    @JsonIgnore
    public boolean hasTLS() {
        return this.transports.contains(Transport.tls);
    }

    @JsonIgnore
    public boolean hasWS() {
        return this.transports.contains(Transport.ws);
    }

    @JsonIgnore
    public boolean hasSCTP() {
        return this.transports.contains(Transport.sctp);
    }

    public List<Transport> getTransports() {
        return this.transports;
    }

    public String getName() {
        return this.name;
    }

    @JsonIgnore
    public SipURI getListeningAddress() {
        return this.listen;
    }

    public SipURI getVipAddress() {
        return this.vipAddress;
    }

}
