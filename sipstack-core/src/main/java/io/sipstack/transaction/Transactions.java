package io.sipstack.transaction;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.transport.Flow;

/**
 * Represents the SIP transaction layer through which one can send messages. Each message
 * will always be associated with a transaction (so there is no method to ask to create
 * a transaction, that will be done automatically. If you want a transaction stateless stack
 * then simply use the raw Netty sip support).
 *
 * @author jonas@jonasborjesson.com
 */
public interface Transactions {

    /**
     * Have the {@link SipMessage} sent through the transaction layer using
     * the specified {@link Flow}. If the {@link Flow} fails,
     * {@link TransactionUser#onIOException(Transaction, SipMessage)} will be called with details about the failure.
     *
     * @param flow
     * @param msg
     * @return
     */
    Transaction send(Flow flow, SipMessage msg);

    /**
     * Have the {@link SipMessage} sent via the transaction layer but without specifying which
     * particular {@link Flow} to use. The result is that the {@link TransportLayer} will examine the {@link SipMessage}
     * and if the next hop indicates a flow to use, then that flow will be used. If not, normal RFC 3263 procedures
     * will be used.
     *
     * @param msg
     * @return
     */
    // Transaction send(SipMessage msg);

    Flow.Builder createFlow(String host) throws IllegalArgumentException;

    default Flow.Builder createFlow(Buffer host) throws IllegalArgumentException {
        return createFlow(host.toString());
    }
}
