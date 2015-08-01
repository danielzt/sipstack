package io.sipstack.transaction;

import io.sipstack.transport.FlowException;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ClientTransaction extends Transaction {

    /**
     * A {@link ClientTransaction} is created by calling {@link TransactionLayer#newClientTransaction(SipRequest)}
     * which will create a new client transaction but will NOT actually send the associated request out.
     * In order for the request to be sent out, call this start method, which will start the entire transaction.
     *
     * @return a reference to 'this'. Just to allow for a fluent interface.
     * @throws FlowException
     */
    ClientTransaction start() throws FlowException;
}
