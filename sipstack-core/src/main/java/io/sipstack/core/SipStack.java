package io.sipstack.core;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.actor.InternalScheduler;
import io.sipstack.config.SipConfiguration;
import io.sipstack.net.InboundOutboundHandlerAdapter;
import io.sipstack.net.NetworkLayer;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.SystemClock;
import io.sipstack.transaction.TransactionLayer;
import io.sipstack.transactionuser.DefaultTransactionUser;
import io.sipstack.transport.Flow;
import io.sipstack.transport.TransportLayer;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipStack {

    void onUpstream(Flow flow, SipMessage message);

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

        public SipStack build() {
            ensureNotNull(scheduler, "You must specify the scheduler");
            final Clock clock = this.clock != null ? this.clock : new SystemClock();

            final TransportLayer transport = new TransportLayer(config.getTransport());
            final TransactionLayer transaction = new TransactionLayer(clock, scheduler, config.getTransaction());
            final DefaultTransactionUser tu = new DefaultTransactionUser(transaction);

            transaction.useTransportLayer(transport);
            transaction.defaultTransactionUser(tu);

            return new DefaultSipStack(transport, transaction, tu);
        }

    }
}
