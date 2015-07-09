package io.sipstack.core;

import io.sipstack.actor.InternalScheduler;
import io.sipstack.config.SipConfiguration;
import io.sipstack.net.InboundOutboundHandlerAdapter;
import io.sipstack.net.NetworkLayer;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.SystemClock;
import io.sipstack.transaction.impl.DefaultTransactionLayer;
import io.sipstack.transactionuser.TransactionEvent;
import io.sipstack.transactionuser.TransactionUserLayer;
import io.sipstack.transactionuser.impl.DefaultTransactionUserLayer;
import io.sipstack.transport.TransportLayer;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;

import java.util.function.Consumer;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipStack {

    TransactionUserLayer getTransactionUserLayer();

    /**
     * Get the handler, which is the entry way into the stack.
     *
     * @return
     */
    InboundOutboundHandlerAdapter handler();

    /**
     * Catch 22 and annoying. The {@link SipStack} needs a reference to the {@link NetworkLayer} but
     * it too needs a reference to the stack (or the {@link InboundOutboundHandlerAdapter} rather).
     *
     * TODO: come up with something nicer and less stupid...
     *
     * @param network
     */
    void useNetworkLayer(NetworkLayer network);

    static Builder withConfiguration(final SipConfiguration config) {
        ensureNotNull(config, "The configuration must not be null");
        return new Builder(config);
    }

    class Builder {

        private final SipConfiguration config;
        private Clock clock;
        private InternalScheduler scheduler;
        private DefaultTransactionUserLayer transactionUserLayer;
        private Consumer<TransactionEvent> consumer;

        private Builder(final SipConfiguration config) {
            this.config = config;
        }

        public Builder withClock(final Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder withScheduler(final InternalScheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Builder withConsumer(final Consumer<TransactionEvent> consumer) {
            this.consumer = consumer;
            return this;
        }

        public SipStack build() {
            ensureNotNull(scheduler, "You must specify the scheduler");
            final Clock clock = this.clock != null ? this.clock : new SystemClock();

            this.transactionUserLayer = new DefaultTransactionUserLayer(consumer);

            final DefaultTransactionLayer transaction = new DefaultTransactionLayer(clock, scheduler,
                    transactionUserLayer, config.getTransaction());
            transactionUserLayer.start(transaction);

            final TransportLayer transport = new TransportLayer(config.getTransport(), transaction);
            transaction.start(transport);

            return new DefaultSipStack(transport, transaction, transactionUserLayer);
        }
    }
}
