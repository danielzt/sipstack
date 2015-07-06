package io.sipstack.core;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.net.InboundOutboundHandlerAdapter;
import io.sipstack.net.NetworkLayer;
import io.sipstack.transaction.TransactionLayer;
import io.sipstack.transactionuser.DefaultTransactionUser;
import io.sipstack.transport.Flow;
import io.sipstack.transport.TransportLayer;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultSipStack implements SipStack {

    private final TransportLayer transportLayer;

    private final TransactionLayer transactionLayer;

    private final DefaultTransactionUser tu;

    DefaultSipStack(final TransportLayer transportLayer, final TransactionLayer transactionLayer, final DefaultTransactionUser tu) {

        // not that great since we cannot guarantee immutability since
        // we are leaking the SipStack to an external entity before
        // the stack is fully initialized. Got to re-structure some of
        // this to avoid this silliness.
        this.transportLayer = transportLayer;
        this.transportLayer.useSipStack(this);

        this.transactionLayer = transactionLayer;
        this.tu = tu;
    }

    @Override
    public void onUpstream(final Flow flow, final SipMessage message) {
        transactionLayer.onUpstream(flow, message);
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
