package io.sipstack.transaction;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
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

    /**
     * When the transaction receives a retransmitted {@link SipResponse}, the specified
     * function will be called.
     *
     * @param f the function that will be called when a re-transmit
     *          happens for this {@link ClientTransaction}. The arguments to
     *          the function is a reference to 'this', i.e. to the same
     *          {@link ClientTransaction} to which you registered the function.
     *          and then the re-transmitted {@link SipResponse}.
     */
    // void onRetransmit(BiConsumer<ClientTransaction, SipResponse> f);

    /**
     * Whenever a {@link SipResponse} is received on this transaction, the
     * supplied function will be called.
     *
     * @param f the function that will be called whenever a {@link SipResponse} is
     *          received for this {@link ClientTransaction}. The arguments to the
     *          function is a reference to 'this', i.e. to the same {@link ClientTransaction}
     *          to which oyu registered the function and then the {@link SipResponse} that
     *          was received.
     */
    // void onResponse(BiConsumer<ClientTransaction, SipResponse> f);

    // void onTransportError(BiConsumer<ClientTransaction, SipRequest> f);
}
