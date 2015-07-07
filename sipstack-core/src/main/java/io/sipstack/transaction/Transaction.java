package io.sipstack.transaction;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Transaction {

    TransactionId id();

    TransactionState state();
}
