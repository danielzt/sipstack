package io.sipstack.transactionuser;

import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.address.Address;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.event.Event;

import java.util.function.BiConsumer;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Proxy {

    /**
     * The {@link SipRequest} that is being proxied.
     *
     * @return
     */
    SipRequest request();

    void start();

    /**
     * Cancel any outstanding branches that we may have
     * and allow the ApplicationInstance to still receive
     * updates of what's going on.
     */
    void cancel();

    /**
     * Same as cancel with the exception that the ApplicationInstance
     * will NOT receive any updates of what's going on. The typical
     * scenario is that for whatever reason you have decided that
     * your io.sipstack.application.application instance should die but you still want the underlying
     * SIP traffic to finish cleanly (such as deal with re-transmissions etc).
     *
     * Your ApplicationInstance will be purged from memory right away with
     * no chance of recovery.
     */
    void terminate();

    /**
     * You just want everything to be cleaned up right away. This will
     * purge everything about this proxy out of memory, including any
     * low-level io.sipstack.transaction.transaction and/or dialog state there still may be.
     * This means that any re-transmissions for a particular io.sipstack.transaction.transaction
     * will not be treated as a completely new request and a new
     * io.sipstack.transaction.transaction will be created. Use this with care.
     */
    // void die();

    interface Builder {

        Builder withParallelForking();

        Builder withSerialForking();

        Builder withSupervision();

        public Builder withNoSupervision();

        Builder withRecordRoute(final Address address);

        Builder onBranchFailure(final BiConsumer<ProxyBranch, Event> consumer);

        /**
         * Create a new {@link ProxyBranch} by indicating the target. The object returned
         * is a {@link ProxyBranch.Builder} that you can use
         * to further configure the branch.
         *
         * Note, even though you will get back a builder, there is no build-method on it
         * because that is done by the {@link Proxy.Builder} when it is built.
         *
         * @param branch
         * @return
         */
        ProxyBranch.Builder withBranch(SipURI target);

        Proxy build();

        /**
         * Convenience method for calling first {@link Builder#build()} followed by {@link Proxy#start()}.
         *
         * @return
         */
        default Proxy start() {
            final Proxy proxy = build();
            proxy.start();
            return proxy;
        }
    }
}

