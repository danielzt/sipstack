package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transport.Flow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultTransactionStore implements TransactionStore {

    private final TransactionLayerConfiguration config;
    private final int stores = 10;
    private final Map<TransactionId, TransactionHolder>[] transactions;
    private final TransactionFactory factory;

    public DefaultTransactionStore(final TransactionFactory factory, final TransactionLayerConfiguration config) {
        this.config = config;
        this.factory = factory;
        transactions = new Map[stores];
        for (int i = 0; i < stores; ++i) {
            transactions[i] = new ConcurrentHashMap<>(config.getDefaultStorageSize() / stores, 0.75f);
        }
    }


    @Override
    public TransactionHolder ensureTransaction(final boolean isUpstream, final Flow flow, final SipMessage sipMsg) {
        final TransactionId id = TransactionId.create(sipMsg);
        return transactions[Math.abs(id.hashCode() % stores)].computeIfAbsent(id, obj -> {

            if (sipMsg.isResponse()) {
                // wtf. Stray response, deal with it
                throw new RuntimeException("Sorry, not dealing with stray responses right now");
            }

            if (sipMsg.isInvite()) {
                if (isUpstream) {
                    return factory.createInviteServerTransaction(id, flow, sipMsg.toRequest(), config);
                } else {
                    return factory.createInviteClientTransaction(id, flow, sipMsg.toRequest(), config);
                }
            }

            // if ack doesn't match an existing transaction then this ack must have been to a 2xx and
            // therefore goes in its own transaction but then ACKs doesn't actually have a real
            // transaction so therefore, screw it...
            if (sipMsg.isAck()) {
                return null;
            }

            if (isUpstream) {
                return factory.createNonInviteServerTransaction(id, flow, sipMsg.toRequest(), config);
            } else {
                throw new RuntimeException("Haven't done the NonInviteClientTransaction just yet");
            }
        });
    }

    @Override
    public TransactionHolder get(final TransactionId id) {
        return transactions[Math.abs(id.hashCode() % stores)].get(id);
    }

    @Override
    public void remove(final TransactionId id) {
        transactions[Math.abs(id.hashCode() % stores)].remove(id);
    }
}
