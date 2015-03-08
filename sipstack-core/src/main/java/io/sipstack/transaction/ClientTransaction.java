/**
 * 
 */
package io.sipstack.transaction;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ClientTransaction extends Transaction {

    @Override
    default boolean isClientTransaction() {
        return true;
    }

    @Override
    default ClientTransaction toClientTransaction() throws ClassCastException {
        return this;
    }

}
