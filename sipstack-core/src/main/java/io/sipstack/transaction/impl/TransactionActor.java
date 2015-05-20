/**
 * 
 */
package io.sipstack.transaction.impl;

import io.hektor.core.Actor;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public interface TransactionActor extends Actor {

    /**
     * The a snapshot of the current transaction as a {@link Transaction} object.
     * 
     * @return
     */
    Transaction getTransaction();

    TransactionId getTransactionId();

}
