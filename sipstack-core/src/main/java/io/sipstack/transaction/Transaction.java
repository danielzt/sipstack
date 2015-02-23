/**
 * 
 */
package io.sipstack.transaction;

import static io.pkts.packet.sip.impl.PreConditions.assertNotNull;
import io.pkts.packet.sip.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Transaction {

    default boolean isClientTransaction() {
        return false;
    }

    default boolean isServerTransaction() {
        return false;
    }

    static Transaction create(final TransactionId id, final SipMessage msg) {
        assertNotNull(id, "Transaction Id cannot be null");
        return null;
    }

}
