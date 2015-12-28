/**
 * 
 */
package io.sipstack.netty.codec.sip;

import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;

import java.net.InetSocketAddress;
import java.util.Arrays;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public interface ConnectionId {

    int getLocalPort();

    byte[] getRawLocalIpAddress();

    String getLocalIpAddress();

    int getRemotePort();

    byte[] getRawRemoteIpAddress();

    String getRemoteIpAddress();

    Transport getProtocol();

    Buffer encode();

    String encodeAsString();

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
        return getProtocol() == Transport.wss;
    }

    static ConnectionId create(final Transport transport, final InetSocketAddress local, final InetSocketAddress remote) {
        ensureNotNull(transport);
        ensureNotNull(local);
        ensureNotNull(remote);

        final byte[] rawLocal = local.getAddress().getAddress();
        final byte[] rawRemote = remote.getAddress().getAddress();
        final int localPort = local.getPort();
        final int remotePort = remote.getPort();
        return new IPv4ConnectionId(transport, rawLocal, localPort, rawRemote, remotePort);
    }


    static ConnectionId decode(final String encoded) {
        final byte[] decoded = new byte[encoded.length() / 2];
        for (int i = 0; i < decoded.length; ++i) {
            final int l = encoded.charAt(i * 2) - 'A';
            final int h = encoded.charAt(i * 2 + 1) - 'A';
            decoded[i] = (byte) ((l << 4 | h) & 0xFF);
        }

        final byte[] remoteIp = new byte[4];
        System.arraycopy(decoded, 0, remoteIp, 0, 4);

        final int remotePort = (decoded[4] & 0xFF) << 24 | (decoded[5] & 0xFF) << 16 | (decoded[6] & 0xFF) << 8
                | decoded[7] & 0xFF;

        final byte[] localIp = new byte[4];
        System.arraycopy(decoded, 8, localIp, 0, 4);

        final int localPort = (decoded[12] & 0xFF) << 24 | (decoded[13] & 0xFF) << 16 | (decoded[14] & 0xFF) << 8
                | decoded[15] & 0xFF;

        Transport protocol = null;
        switch (decoded[16]) {
            case 0x01:
                protocol = Transport.udp;
                break;
            case 0x02:
                protocol = Transport.tcp;
                break;
            case 0x03:
                protocol = Transport.tls;
                break;
            case 0x04:
                protocol = Transport.ws;
                break;
            case 0x05:
                protocol = Transport.wss;
                break;
            case 0x06:
                protocol = Transport.sctp;
                break;
        }
        return new IPv4ConnectionId(protocol, localIp, localPort, remoteIp, remotePort);
    }

    class IPv4ConnectionId implements ConnectionId {

        private final Transport protocol;
        private final int localPort;
        private final int remotePort;
        private final byte[] remoteIp;
        private final byte[] localIp;

        /**
         * The encoded version of this connection id. 
         * 
         * Performance testing showed that there was actually
         * a slight benefit keeping these cached.
         */
        private final String encodedAsString;
        private final Buffer encodedAsBuffer;
        private final int hashCode;
        private final String humanReadableString;

        private IPv4ConnectionId(final Transport protocol, final byte[] localIp, final int localPort,
                final byte[] remoteIp, final int remotePort) {
            this.localIp = localIp;
            this.localPort = localPort;
            this.remoteIp = remoteIp;
            this.remotePort = remotePort;
            this.protocol = protocol;

            this.hashCode = calculateHashCode();
            this.encodedAsString = encodeConnection();
            this.encodedAsBuffer = Buffers.wrap(encodedAsString);
            final StringBuilder sb = new StringBuilder(convertToStringIP(localIp));
            sb.append(":").append(localPort);
            sb.append(":").append(protocol);
            sb.append(":").append(convertToStringIP(remoteIp));
            sb.append(":").append(remotePort);
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
            result = prime * result + Arrays.hashCode(this.localIp);
            result = prime * result + this.localPort;
            result = prime * result + Arrays.hashCode(this.remoteIp);
            result = prime * result + this.remotePort;
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
            final IPv4ConnectionId other = (IPv4ConnectionId) obj;
            if (this.protocol != other.protocol) {
                return false;
            }

            if (this.localPort != other.localPort) {
                return false;
            }
            if (this.remotePort != other.remotePort) {
                return false;
            }
            if (!Arrays.equals(this.localIp, other.localIp)) {
                return false;
            }
            if (!Arrays.equals(this.remoteIp, other.remoteIp)) {
                return false;
            }
            return true;
        }

        @Override
        public String encodeAsString() {
            return this.encodedAsString;
        }

        @Override
        public Buffer encode() {
            return this.encodedAsBuffer;
        }

        private String encodeConnection() {
            final byte[] toEncode = new byte[17];
            System.arraycopy(this.remoteIp, 0, toEncode, 0, this.remoteIp.length);

            toEncode[4] = (byte) (this.remotePort >>> 24);
            toEncode[5] = (byte) (this.remotePort >>> 16);
            toEncode[6] = (byte) (this.remotePort >>> 8);
            toEncode[7] = (byte) this.remotePort;

            System.arraycopy(this.localIp, 0, toEncode, 8, this.localIp.length);

            toEncode[12] = (byte) (this.localPort >>> 24);
            toEncode[13] = (byte) (this.localPort >>> 16);
            toEncode[14] = (byte) (this.localPort >>> 8);
            toEncode[15] = (byte) this.localPort;
            if (Transport.udp == this.protocol) {
                toEncode[16] = 0x01;
            } else if (Transport.tcp == this.protocol) {
                toEncode[16] = 0x02;
            } else if (Transport.tls == this.protocol) {
                toEncode[16] = 0x03;
            } else if (Transport.ws == this.protocol) {
                toEncode[16] = 0x04;
            } else if (Transport.wss == this.protocol) {
                toEncode[16] = 0x05;
            } else if (Transport.sctp == this.protocol) {
                toEncode[16] = 0x06;
            } else {
                toEncode[16] = 0x00;
            }

            return new String(translate(toEncode));
        }

        private char[] translate(final byte[] values) {
            int pos = 0;
            final char[] array = new char[values.length * 2];
            for (final byte value : values) {
                array[pos++] = ALPHABET[value >>> 4 & 0x0F];
                array[pos++] = ALPHABET[value & 0x0f];
            }

            return array;
        }

        /**
         * Not that I did any performance measuring but just seemed more efficient
         * to have an alphabet that was continuous so when translating back you
         * don't have to do some other weird lookup.
         * 
         */
        private static final char[] ALPHABET = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
            'O', 'P' };

        @Override
        public int getLocalPort() {
            return this.localPort;
        }

        @Override
        public byte[] getRawLocalIpAddress() {
            return this.localIp;
        }

        @Override
        public String getLocalIpAddress() {
            return convertToStringIP(this.localIp);
        }

        @Override
        public int getRemotePort() {
            return this.remotePort;
        }

        @Override
        public byte[] getRawRemoteIpAddress() {
            return this.remoteIp;
        }

        @Override
        public String getRemoteIpAddress() {
            return convertToStringIP(this.remoteIp);
        }

        @Override
        public Transport getProtocol() {
            return this.protocol;
        }

        private String convertToStringIP(final byte[] ip) {
            final short a = (short) (ip[0] & 0xFF);
            final short b = (short) (ip[1] & 0xFF);
            final short c = (short) (ip[2] & 0xFF);
            final short d = (short) (ip[3] & 0xFF);
            return a + "." + b + "." + c + "." + d;
        }
    }

}
