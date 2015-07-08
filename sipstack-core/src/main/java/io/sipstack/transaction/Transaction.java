package io.sipstack.transaction;

import io.sipstack.transport.Flow;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Transaction {

    TransactionId id();

    TransactionState state();

    /**
     * The {@link Flow} associated with this {@link Transaction}.
     *
     * Every {@link Transaction} will always have a flow associated with
     * it but a flow can die. If a flow will die, the transport layer will
     * try and re-establish that flow but may of course not succeed.
     *
     * TODO: show how to correctly handle a flow recovery scenario etc.
     *
     * @return
     */
    Flow flow();
}
