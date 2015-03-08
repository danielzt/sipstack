/**
 * 
 */
package io.sipstack.transaction.impl;

import io.sipstack.transaction.ServerTransaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public class ServerTransactionImpl extends TransactionImpl implements ServerTransaction {

    /**
     * @param id
     * @param state
     */
    public ServerTransactionImpl(final TransactionId id, final TransactionState state) {
        super(id, state);
    }

}
