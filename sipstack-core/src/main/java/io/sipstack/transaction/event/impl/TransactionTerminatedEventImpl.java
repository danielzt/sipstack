package io.sipstack.transaction.event.impl;

import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.event.TransactionTerminatedEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public class TransactionTerminatedEventImpl extends TransactionEventImpl implements TransactionTerminatedEvent {

    public TransactionTerminatedEventImpl(final Transaction transaction) {
        super(transaction);
    }
}
