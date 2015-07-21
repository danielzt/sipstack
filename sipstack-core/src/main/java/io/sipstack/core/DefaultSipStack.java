package io.sipstack.core;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;

import io.sipstack.net.InboundOutboundHandlerAdapter;
import io.sipstack.net.NetworkLayer;
import io.sipstack.transaction.impl.DefaultTransactionLayer;
import io.sipstack.transactionuser.TransactionUserLayer;
import io.sipstack.transactionuser.impl.DefaultTransactionUserLayer;
import io.sipstack.transport.impl.DefaultTransportLayer;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultSipStack implements SipStack {

    private final DefaultTransportLayer transportLayer;

    private final DefaultTransactionLayer transactionLayer;

    private final DefaultTransactionUserLayer tu;

    DefaultSipStack(final DefaultTransportLayer transportLayer, final DefaultTransactionLayer transactionLayer, final DefaultTransactionUserLayer tu) {

        // not that great since we cannot guarantee immutability since
        // we are leaking the SipStack to an external entity before
        // the stack is fully initialized. Got to re-structure some of
        // this to avoid this silliness.
        this.transportLayer = transportLayer;
        this.transactionLayer = transactionLayer;
        this.tu = tu;
    }

    @Override
    public TransactionUserLayer getTransactionUserLayer() {
        return tu;
    }

    @Override
    public InboundOutboundHandlerAdapter handler() {
        return transportLayer;
    }

    @Override
    public void useNetworkLayer(final NetworkLayer network) {
        ensureNotNull(network, "The network layer cannot be null");
        transportLayer.useNetworkLayer(network);
    }

}
