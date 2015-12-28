package io.sipstack.transaction.event.impl;

import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.event.TransactionEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class TransactionEventImpl implements TransactionEvent {
    final Transaction transaction;

    public TransactionEventImpl(final Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public Transaction transaction() {
        return transaction;
    }
}
