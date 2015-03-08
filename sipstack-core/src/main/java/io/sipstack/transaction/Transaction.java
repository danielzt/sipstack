/**
 * 
 */
package io.sipstack.transaction;


/**
 * @author jonas@jonasborjesson.com
 */
public interface Transaction {

    default boolean isClientTransaction() {
        return false;
    }

    default boolean isServerTransaction() {
        return false;
    }

    default ServerTransaction toServerTransaction() throws ClassCastException {
        throw new ClassCastException("Cannot cast object " + ClientTransaction.class.getName() + "into "
                + ServerTransaction.class.getName());
    }

    default ClientTransaction toClientTransaction() throws ClassCastException {
        throw new ClassCastException("Cannot cast object " + ServerTransaction.class.getName() + "into "
                + ClientTransaction.class.getName());
    }

    TransactionId getTransactionId();

    TransactionState getState();

    /**
     * Cancel this transaction, which only applies to an INVITE client transaction. In all other
     * cases this is the same as a no-op.
     */
    default void cancel() {
        // do nothing
    }

}
