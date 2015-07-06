package io.sipstack.transaction;


import io.sipstack.actor.Actor;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionActor extends Actor {

    TransactionId id();

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
