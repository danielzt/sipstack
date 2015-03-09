/**
 * 
 */
package io.sipstack.transaction;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public enum TransactionState {
    INIT, TRYING, PROCEEDING, COMPLETED, CONFIRMED, TERMINATED;
}
