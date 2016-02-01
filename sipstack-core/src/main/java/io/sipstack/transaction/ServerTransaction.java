package io.sipstack.transaction;

import io.netty.channel.ChannelHandlerContext;
import io.pkts.packet.sip.SipRequest;

import java.util.function.BiConsumer;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ServerTransaction extends Transaction {

    default boolean isServerTransaction() {
        return true;
    }

    default ServerTransaction toServerTransaction() {
        return this;
    }

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
    default void onRetransmit(BiConsumer<ServerTransaction, SipRequest> f) {
        // left empty while I figure out the API
    }

    // do we need a re-transmit function when we re-transmit happens in a Invite Server Transaction
    // due to no ACK? Should we perhaps have a separate transaction interface for Invite Server Transactions?

}
