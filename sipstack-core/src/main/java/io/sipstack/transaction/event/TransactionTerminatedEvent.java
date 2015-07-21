package io.sipstack.transaction.event;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionTerminatedEvent extends TransactionLifeCycleEvent {

    @Override
    default boolean isTransactionTerminatedEvent() {
        return true;
    }

    @Override
    default TransactionTerminatedEvent toTransactionTerminatedEvent() {
        return this;
    }

}
