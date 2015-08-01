package io.sipstack.transaction.impl;


import io.sipstack.actor.Actor;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionActor extends Actor {

    TransactionId id();

    TransactionState state();

    boolean isClientTransaction();

    default boolean isServerTransaction() {
        return !isClientTransaction();
    }

    /**
     * Get a state representation of the current io.sipstack.transaction.transaction.
     *
     * The actor is what actually implements the FSM but if
     * someone wants to interact with the actor it has to go
     * through the io.sipstack.transaction.transaction interface, which is an immutable
     * representation of the current state of the actor.
     *
     * @return
     */
    // Transaction transaction();

}
