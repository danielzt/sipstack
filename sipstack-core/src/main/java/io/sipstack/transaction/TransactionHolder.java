package io.sipstack.transaction;

import io.sipstack.transport.Flow;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionHolder extends Transaction {

    TransactionActor actor();

    Optional<Flow> flow();
}
