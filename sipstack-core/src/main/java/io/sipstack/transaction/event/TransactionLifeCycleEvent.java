package io.sipstack.transaction.event;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionLifeCycleEvent extends TransactionEvent {

    @Override
    default boolean isTransactionLifeCycleEvent() {
        return true;
    }

    @Override
    default TransactionLifeCycleEvent toTransactionLifeCycleEvent() {
        return this;
    }

}
