/**
 * 
 */
package io.sipstack.actor;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.ConnectionId;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Key {

    static Key withBuffer(final Buffer buffer) {
        return new DefaultKey(buffer.hashCode());
    }

    static Key withConnectionId(final ConnectionId id) {
        return new ConnectionIdKey(id);
    }


    /**
     * Convenience method for creating a new {@link Key} based on a {@link SipMessage}.
     * 
     * @param msg
     * @return
     */
    static Key withSipMessage(final SipMessage msg) {
        final Buffer callId = msg.getCallIDHeader().getValue();
        return Key.withBuffer(callId);
    }

    /**
     * A {@link Key} that has its hash code directly defined.
     */
    static class DefaultKey implements Key {
        private final int hashCode;

        private DefaultKey(final int hashCode) {
            this.hashCode = hashCode;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return hashCode;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            try {
                final DefaultKey other = (DefaultKey) obj;
                return hashCode == other.hashCode;
            } catch (final ClassCastException e) {
                return false;
            }
        }
    }

    static class ConnectionIdKey implements Key {
        private final ConnectionId id;

        private ConnectionIdKey(final ConnectionId id) {
            this.id = id;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return this.id.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            try {
                final ConnectionIdKey other = (ConnectionIdKey) obj;
                return this.id.equals(other.id);
            } catch (final ClassCastException e) {
                return false;
            }
        }
    }


}
