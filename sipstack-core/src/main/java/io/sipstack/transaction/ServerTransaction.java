package io.sipstack.transaction;

import io.pkts.packet.sip.SipRequest;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ServerTransaction extends Transaction {

    /**
     * When the transaction receives a retransmitted {@link SipRequest}, the specified
     * function will be called.
     *
     * @param f the function that will be called when a re-transmit
     *          happens for this {@link ServerTransaction}. The arguments to
     *          the function is a reference to 'this', i.e. to the same
     *          {@link ServerTransaction} to which you registered the function.
     *          and then the re-transmitted {@link SipRequest}.
     */
    // void onRetransmit(BiConsumer<ServerTransaction, SipRequest> f);

}
