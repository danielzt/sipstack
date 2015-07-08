package io.sipstack.transaction.impl;

import io.sipstack.transaction.Transaction;
import io.sipstack.transport.Flow;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionHolder extends Transaction {

    TransactionActor actor();

    Flow flow();
}
