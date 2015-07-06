/**
 * 
 */
package io.sipstack.transaction;

/**
 * Represents all the states a io.sipstack.transaction.transaction may be in, Invite and Non-Invite transactions
 * alike. Of course, a Non-Invite io.sipstack.transaction.transaction has less states that an Invite io.sipstack.transaction.transaction.
 *
 * @author jonas@jonasborjesson.com
 */
public enum TransactionState {
    INIT, TRYING, PROCEEDING, ACCEPTED, COMPLETED, CONFIRMED, TERMINATED;
}
