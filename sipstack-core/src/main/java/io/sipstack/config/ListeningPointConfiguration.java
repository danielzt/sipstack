package io.sipstack.config;

import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipParseException;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.impl.PreConditions;
import io.pkts.packet.sip.impl.SipParser;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by jonas@jonasborjesson.com
 */
public class ListeningPointConfiguration {

    /**
     * The IP-address that we will bind to.
     */
    @JsonIgnore
    private final SipURI listeningPoint;

    /**
     * In those cases where you have some kind of VIP address and even though
     * you are technically not listening to this address you wish this address
     * to be the one used in Record-Route and Via-headers.
     */
    @JsonIgnore
    private final SipURI vipAddress;

    @JsonIgnore
    public Buffer getIp() {
        return this.listeningPoint.getHost();
    }

    @JsonIgnore
    public int getPort() {
        return this.listeningPoint.getPort();
    }

    @JsonIgnore
    public Buffer getTransport() {
        final Buffer transport = this.listeningPoint.getTransportParam();
        if (transport != null) {
            return transport;
        }
        return Buffers.EMPTY_BUFFER;
    }

    @JsonIgnore
    public boolean isUDP() {
        return SipParser.isUDPLower(getTransport());
    }

    @JsonIgnore
    public boolean isTCP() {
        return SipParser.isTCPLower(getTransport());
    }

    @JsonIgnore
    public boolean isTLS() {
        return SipParser.isTLSLower(getTransport());
    }

    @JsonIgnore
    public boolean isWS() {
        return SipParser.isWSLower(getTransport());
    }

    @JsonIgnore
    public boolean isSCTP() {
        return SipParser.isSCTPLower(getTransport());
    }

    @JsonIgnore
    public Buffer getVipIP() {
        if (this.vipAddress != null) {
            return this.vipAddress.getHost();
        }
        return Buffers.EMPTY_BUFFER;
    }

    @JsonIgnore
    public int getVipPort() {
        if (this.vipAddress != null) {
            return this.vipAddress.getPort();
        }
        return -1;
    }

    private ListeningPointConfiguration(final SipURI listeningPoint, final SipURI vipAddress) {
        this.listeningPoint = listeningPoint;
        this.vipAddress = vipAddress;
    }

    /**
     * Create a new ListeningPointConfiguration based on the supplied value.
     *
     * @param value
     * @return
     */
    @JsonCreator
    public static ListeningPointConfiguration create(final String value) throws SipParseException {
        PreConditions.assertNotEmpty(value, "The listening point configuration cannot be null or empty");

        final String[] values = value.split(" ");

        if (values.length == 2 || values.length > 3) {
            throw new SipParseException(values[0].length(), "Bad format. Accepted formats <sip uri> ['as' <sip uri>]");
        }

        try {
            final SipURI uri = SipURI.frame(ensureSipURI(values[0]));
            SipURI asURI = null;
            if (values.length == 3) {
                if (!"as".equalsIgnoreCase(values[1])) {
                    throw new SipParseException(values[0].length() + 1, "Bad format. Accepted formats <sip uri> ['as' <sip uri>]");
                }
                asURI = SipURI.frame(ensureSipURI(values[2]));
            }
            return new ListeningPointConfiguration(uri, asURI);
        } catch (final IOException e) {
            throw new SipParseException(0, "Unable to parse value due to an IOException", e);
        }
    }

    private static Buffer ensureSipURI(final String value) {
        if (value.startsWith("sip")) {
            return Buffers.wrap(value.trim());
        }
        return Buffers.wrap("sip:" + value.trim());
    }
}
