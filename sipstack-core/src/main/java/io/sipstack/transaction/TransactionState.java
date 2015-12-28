/**
 * 
 */
package io.sipstack.transaction;

/**
 * Represents all the states a transaction may be in, Invite and Non-Invite transactions
 * alike. Of course, a Non-Invite transaction has less states that an Invite transaction.
 *
 * @author jonas@jonasborjesson.com
 */
public enum TransactionState {
    INIT, CALLING, TRYING, PROCEEDING, ACCEPTED, COMPLETED, CONFIRMED, TERMINATED;
}
