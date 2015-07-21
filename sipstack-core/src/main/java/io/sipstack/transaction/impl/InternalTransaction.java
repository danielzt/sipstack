package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.transaction.Transaction;
import io.sipstack.transport.Flow;
import io.sipstack.transport.impl.DefaultTransportLayer;

/**
 * @author jonas@jonasborjesson.com
 */
public interface InternalTransaction extends Transaction {

    /**
     * When a message is read off of the network (and passed through the the {@link DefaultTransportLayer} etc)
     * it will eventually be delivered to a {@link Transaction} via this method. Hence, the "direction"
     * of the message is "upstream" in the stack.
     *
     * @param message
     */
    void onUpstreamMessage(Flow flow, SipMessage message);

    TransactionActor actor();
}
