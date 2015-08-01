package io.sipstack.transaction;

import io.pkts.packet.sip.SipResponse;
import io.sipstack.transport.Flow;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Transaction {

    TransactionId id();

    TransactionState state();

    /**
     * Send a sip message within this {@link Transaction}. If the message that is about to be sent
     * is not actually part of this transaction then an {@link IllegalArgumentException} will
     * be thrown.
     *
     * @param msg
     * @throws
     */
    default void send(final SipResponse response) throws IllegalArgumentException {
        throw new IllegalArgumentException("Only a ServerTransaction allows you to send a response");
    }

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
