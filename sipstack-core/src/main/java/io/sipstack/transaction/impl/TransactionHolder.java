package io.sipstack.transaction.impl;

import io.sipstack.transaction.Transaction;
import io.sipstack.transport.Flow;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionHolder extends Transaction {

    TransactionActor actor();

    Optional<Flow> flow();
}
