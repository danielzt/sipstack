package io.sipstack.netty.codec.sip;

import io.pkts.packet.sip.Transport;
import io.pkts.packet.sip.impl.PreConditions;

import java.net.InetSocketAddress;
import java.util.Arrays;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;

/**
 * A {@link ConnectionEndpointId} is closely related to a {@link ConnectionId} and is
 * in fact "half" of the regular {@link ConnectionId} in that it only represents the
 * transport + either the local or remote IP:port. I.e., an endpoint.
 *
 * The {@link ConnectionEndpointId} is useful when you want to store
 * a connection under a key that just takes the e.g. remote party
 * into consideration.
 *
 * @author jonas@jonasborjesson.com
 */
public interface ConnectionEndpointId {

    int getPort();

    byte[] getRawIpAddress();

    String getIpAddress();

    Transport getProtocol();

    InetSocketAddress getAddress();

    default boolean isReliableTransport() {
        return getProtocol() != Transport.udp;
    }

    default boolean isUDP() {
        return getProtocol() == Transport.udp;
    }

    default boolean isTCP() {
        return getProtocol() == Transport.tcp;
    }

    default boolean isTLS() {
        return getProtocol() == Transport.tls;
    }

    default boolean isSCTP() {
        return getProtocol() == Transport.sctp;
    }

    default boolean isWS() {
        return getProtocol() == Transport.ws;
    }

    default boolean isWSS() {
        return getProtocol() == Transport.ws;
    }

    static ConnectionEndpointId create(final Transport transport, final InetSocketAddress address) {
        ensureNotNull(transport);
        ensureNotNull(address);

        final byte[] rawAddress = address.getAddress().getAddress();
        final int port = address.getPort();
        return new IPv4ConnectionEndpointId(transport, address, rawAddress, port);
    }

    static ConnectionEndpointId create(final Transport transport, final byte[] rawAddress, final int port) {
        final InetSocketAddress address = new InetSocketAddress(ConnectionId.convertToStringIP(rawAddress), port);
        return new IPv4ConnectionEndpointId(transport, address, rawAddress, port);
    }

    static ConnectionEndpointId create(final Transport transport,
                                       final InetSocketAddress address,
                                       final byte[] rawAddress,
                                       final int port) {
        ensureNotNull(transport);
        ensureNotNull(address);
        return new IPv4ConnectionEndpointId(transport, address, rawAddress, port);
    }

    class IPv4ConnectionEndpointId implements ConnectionEndpointId {

        private final InetSocketAddress address;
        private final Transport protocol;
        private final int port;
        private final byte[] ip;

        private final int hashCode;
        private final String humanReadableString;

        private IPv4ConnectionEndpointId(final Transport protocol,
                                         final InetSocketAddress address,
                                         final byte[] ip,
                                         final int port) {
            this.address = address;
            this.protocol = protocol;
            this.ip = ip;
            this.port = port;
            this.hashCode = calculateHashCode();
            final StringBuilder sb = new StringBuilder(protocol.toString());
            sb.append(":").append(ConnectionId.convertToStringIP(ip));
            sb.append(":").append(port);
            this.humanReadableString = sb.toString();
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }

        private int calculateHashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.protocol.hashCode();
            result = prime * result + Arrays.hashCode(this.ip);
            result = prime * result + this.port;
            return result;
        }

        @Override
        public String toString() {
            return this.humanReadableString;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final IPv4ConnectionEndpointId other = (IPv4ConnectionEndpointId) obj;
            if (this.protocol != other.protocol) {
                return false;
            }

            if (this.port != other.port) {
                return false;
            }

            if (!Arrays.equals(this.ip, other.ip)) {
                return false;
            }
            return true;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public byte[] getRawIpAddress() {
            return ip;
        }

        @Override
        public String getIpAddress() {
            return ConnectionId.convertToStringIP(ip);
        }

        @Override
        public Transport getProtocol() {
            return protocol;
        }

        @Override
        public InetSocketAddress getAddress() {
            return address;
        }
    }

}
