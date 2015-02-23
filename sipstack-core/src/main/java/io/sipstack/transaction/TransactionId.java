/**
 * 
 */
package io.sipstack.transaction;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.header.ViaHeader;
import io.pkts.packet.sip.impl.PreConditions;

import java.util.Arrays;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionId {

    static TransactionId create(final SipMessage msg) throws IllegalArgumentException {
        PreConditions.ensureNotNull(msg, "SIP message cannot be null");
        final ViaHeader via = msg.getViaHeader();
        PreConditions.ensureNotNull(via, "No Via-header found in the SIP message");
        final Buffer branch = via.getBranch();
        final int capacity = branch.capacity();
        final int length = msg.isCancel() ? capacity + 7 : capacity;
        final byte[] id = new byte[length];
        branch.getByes(id);
        if (msg.isCancel()) {
            id[capacity + 0] = '-';
            id[capacity + 1] = 'C';
            id[capacity + 2] = 'A';
            id[capacity + 3] = 'N';
            id[capacity + 4] = 'C';
            id[capacity + 5] = 'E';
            id[capacity + 6] = 'L';

        }
        return new TransactionIdImpl(id);
    }

    static final class TransactionIdImpl implements TransactionId {
        private final byte[] id;

        private TransactionIdImpl(final byte[] id) {
            this.id = id;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Arrays.hashCode(id);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            try {
                final TransactionIdImpl other = (TransactionIdImpl) obj;
                return Arrays.equals(id, other.id);
            } catch (ClassCastException | NullPointerException e) {
                return false;
            }
        }

    }

}
