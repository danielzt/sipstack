package io.sipstack.netty.codec.sip.transaction;

import io.netty.channel.ChannelHandlerContext;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.InboundOutboundHandlerAdapter;
import io.sipstack.netty.codec.sip.config.TransactionLayerConfiguration;
import io.sipstack.netty.codec.sip.event.Event;
import io.sipstack.netty.codec.sip.event.SipMessageEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jonas@jonasborjesson.com
 */
public class TransactionLayer extends InboundOutboundHandlerAdapter {

    private final TransactionLayerConfiguration config;

    // TODO: This need to be configurable. Also, the JDK map implementation may
    // not be the fastest around either so do some performance tests regarding
    // that...
    private final Map<TransactionId, Transaction> transactions = new ConcurrentHashMap<>(1000, 0.75f);

    public TransactionLayer(final TransactionLayerConfiguration config) {
        this.config = config;
    }

    /**
     * We only expect {@link SipMessageEvent}s here since there will always be a
     * decoder in-front of this one.
     */
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        try {
            final Event event = (Event)msg;
            final SipMessage sipMsg = event.toSipMessageEvent().message();
            final TransactionId id = TransactionId.create(sipMsg);
            final Transaction transaction = transactions.computeIfAbsent(id, obj -> {

                if (sipMsg.isResponse()) {
                    // wtf. Stray response, deal with it
                    throw new RuntimeException("Sorry, not dealing with stray responses right now");
                }

                if (sipMsg.isInvite()) {
                    return new InviteServerTransaction();
                }

                // if ack doesn't match an existing transaction then this ack must have been to a 2xx and
                // therefore goes in its own transaction but then ACKs doesn't actually have a real
                // transaction so therefore, screw it...
                if (sipMsg.isAck()) {
                    return null;
                }

                return new NonInviteServerTransaction();
            });

            if (transaction != null) {
                invokeTransaction(ctx, event, transaction);
            } else {
                // may have been a stray response or an ACK to a 2xx
                // and we should probably forward them up the chain...
                ctx.fireChannelRead(msg);
            }

        } catch (final ClassCastException e) {
            // strange...
        }
    }

    private void invokeTransaction(final ChannelHandlerContext ctx,
                                   final Event event,
                                   final Transaction transaction) {

        transaction.onEvent(null, event);
    }

}
