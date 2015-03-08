/**
 * 
 */
package io.sipstack.transaction.impl;

import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;

/**
 * @author jonas
 *
 */
public class TransactionImpl implements Transaction {

    private final TransactionId id;
    private final TransactionState state;

    /**
     * 
     */
    public TransactionImpl(final TransactionId id, final TransactionState state) {
        this.id = id;
        this.state = state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionId getTransactionId() {
        return this.id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionState getState() {
        return this.state;
    }

}
