package io.sipstack.netty.codec.sip.transaction;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Transaction {

    TransactionId id();

    TransactionState state();

}
