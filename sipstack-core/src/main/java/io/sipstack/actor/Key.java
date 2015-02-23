/**
 * 
 */
package io.sipstack.actor;

import io.pkts.buffer.Buffer;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Key {

    static Key withBuffer(final Buffer buffer) {
        return new DefaultKey(buffer.hashCode());
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

}
