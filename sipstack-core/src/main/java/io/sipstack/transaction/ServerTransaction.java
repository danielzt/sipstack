/**
 * 
 */
package io.sipstack.transaction;

/**
 * @author jonas
 *
 */
public interface ServerTransaction extends Transaction {

    @Override
    default boolean isServerTransaction() {
        return true;
    }

}
