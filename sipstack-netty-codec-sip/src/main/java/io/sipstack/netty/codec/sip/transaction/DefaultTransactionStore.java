package io.sipstack.netty.codec.sip.transaction;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.config.TransactionLayerConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultTransactionStore implements TransactionStore {

    private final TransactionLayerConfiguration config;
    private final int stores = 10;
    private final Map<TransactionId, TransactionActor>[] transactions;

    public DefaultTransactionStore(final TransactionLayerConfiguration config) {
        this.config = config;
        transactions = new Map[stores];
        for (int i = 0; i < stores; ++i) {
            transactions[i] = new ConcurrentHashMap<>(config.getDefaultStorageSize() / stores, 0.75f);
        }
    }


    @Override
    public TransactionActor ensureTransaction(final SipMessage sipMsg) {
        final TransactionId id = TransactionId.create(sipMsg);
        return transactions[Math.abs(id.hashCode() % stores)].computeIfAbsent(id, obj -> {

            if (sipMsg.isResponse()) {
                // wtf. Stray response, deal with it
                throw new RuntimeException("Sorry, not dealing with stray responses right now");
            }

            if (sipMsg.isInvite()) {
                return new InviteServerTransactionActor(id, sipMsg.toRequest(), config);
            }

            // if ack doesn't match an existing transaction then this ack must have been to a 2xx and
            // therefore goes in its own transaction but then ACKs doesn't actually have a real
            // transaction so therefore, screw it...
            if (sipMsg.isAck()) {
                return null;
            }

            return new NonInviteServerTransactionActor(id, sipMsg.toRequest(), config);
        });
    }

    @Override
    public TransactionActor get(final TransactionId id) {
        return transactions[Math.abs(id.hashCode() % stores)].get(id);
    }

    @Override
    public void remove(TransactionId id) {
        transactions[Math.abs(id.hashCode() % stores)].remove(id);
    }
}
