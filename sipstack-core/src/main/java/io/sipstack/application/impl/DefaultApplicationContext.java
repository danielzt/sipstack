package io.sipstack.application.impl;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.address.Address;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.address.URI;
import io.pkts.packet.sip.header.ViaHeader;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.application.ApplicationController;
import io.sipstack.application.B2BUA;
import io.sipstack.application.SipEvent;
import io.sipstack.application.SipRequestEvent;
import io.sipstack.application.UA;
import io.sipstack.event.Event;
import io.sipstack.transactionuser.DefaultProxy;
import io.sipstack.transactionuser.DefaultProxyBranch;
import io.sipstack.transactionuser.Dialog;
import io.sipstack.transactionuser.Proxy;
import io.sipstack.transactionuser.ProxyBranch;
import io.sipstack.transactionuser.TransactionEvent;
import io.sipstack.transactionuser.TransactionUserLayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * NOTE: everything within this class ASSUMES that while invoked the invoker, which
 * should ONLY ever be the {@link ApplicationController}, is holding some sort mutex
 * to protect this object and to make all operations within thread safe etc.
 * Hence, NO extra protection is done within this class
 * itself so therefore e.g. doing the following is expected to be safe:
 *
 * <pre>
 *     if (!proxies.containsKey(key))
 *         ...
 *         proxies.put(key, proxy);
 *     }
 * </pre>
 *
 * I.e., there is no way that some other thread will be able to get in between
 * those two lines.
 *
 * @author jonas@jonasborjesson.com
 */
public class DefaultApplicationContext implements InternalApplicationContext {

    private Map<String, DefaultProxy> proxies;
    private Map<String, DefaultUA> uaStore;
    private Map<String, DefaultB2BUA> b2buaStore;

    private TransactionEvent currentEvent;

    private final TransactionUserLayer tu;

    public DefaultApplicationContext(final TransactionUserLayer tu) {
        this.tu = tu;
    }

    @Override
    public Optional<Proxy> getProxyByName(final String friendlyName) {
        return Optional.ofNullable(proxies != null ? proxies.get(friendlyName) : null);
    }

    @Override
    public Proxy.Builder proxyWithFriendlyName(final String friendlyName) {
        if (proxies != null && proxies.containsKey(friendlyName)) {
            throw new IllegalArgumentException(String.format("A proxy with name {} already exists", friendlyName));
        }

        return new ProxyBuilder(friendlyName, currentEvent.message().toRequest());
    }

    @Override
    public Proxy.Builder proxy(final SipURI target) {
        // TODO: need to come up with a strategy for creating
        // default names.
        final Proxy.Builder builder = proxyWithFriendlyName("default");
        builder.withBranch(target);
        return builder;
    }

    @Override
    public UA.Builder uaWithFriendlyName(final String friendlyName) {
        if (uaStore != null && uaStore.containsKey(friendlyName)) {
            throw new IllegalArgumentException(String.format("A ua with name {} already exists", friendlyName));
        }

        return new UABuilder(friendlyName);
    }

    @Override
    public B2BUA.Builder b2buaWithFriendlyName(final String friendlyName) {
        if (b2buaStore != null && b2buaStore.containsKey(friendlyName)) {
            throw new IllegalArgumentException(String.format("A b2bua with name {} already exists", friendlyName));
        }

        return new B2BUABuilder(friendlyName);
    }

    /**
     * When the proxy builder has been built it will register with
     * the context in order to indicate that it is ready for use.
     *
     * @param proxy
     */
    private void registerProxy(final DefaultProxy proxy) {
        ensureProxyStore();
        proxies.put(proxy.friendlyName(), proxy);
    }

    private void ensureProxyStore() {
        if (proxies == null) {
            proxies = new HashMap<>(4);
        }
    }

    /**
     * When the ua builder has been built it will register with
     * the context in order to indicate that it is ready for use.
     *
     * @param ua
     */
    private void registerUA(final DefaultUA ua) {
        ensureUAStore();
        uaStore.put(ua.friendlyName(), ua);
    }

    private void ensureUAStore() {
        if (uaStore == null) {
            uaStore = new HashMap<>(4);
        }
    }

    /**
     * When the b2bua builder has been built it will register with
     * the context in order to indicate that it is ready for use.
     *
     * @param b2bua
     */
    private void registerB2BUA(final DefaultB2BUA b2bua) {
        ensureB2BUAStore();
        b2buaStore.put(b2bua.friendlyName(), b2bua);
    }

    private void ensureB2BUAStore() {
        if (b2buaStore == null) {
            b2buaStore = new HashMap<>(4);
        }
    }

    @Override
    public void preInvoke(final TransactionEvent event) {
        this.currentEvent = event;
    }

    /**
     * Invoked by the {@link ApplicationController} after the io.sipstack.application.application invocation
     * is done and it is at this point we will actually execute the wishes
     * of the user.
     */
    @Override
    public void postInvoke() {

        // start any proxy objects that hasn't been started yet
        if (proxies != null) {
            proxies.values().forEach(DefaultProxy::actuallyStart);
        }

    }

    private class ProxyBranchBuilder implements ProxyBranch.Builder {

        private final SipURI target;
        private final SipRequest request;
        private int priority = 10;
        private int weight = 10;

        private ProxyBranchBuilder(final SipURI target, final SipRequest request) {
            this.target = target;
            this.request = request;
        }

        @Override
        public ProxyBranch.Builder withPriority(int priority) {
            return null;
        }

        @Override
        public ProxyBranch.Builder withWeight(int weight) {
            return null;
        }

        @Override
        public ProxyBranch.Builder onFailure(Consumer<Event> consumer) {
            return null;
        }

        private ProxyBranch build() {
            final Buffer branchId = ViaHeader.generateBranch();
            return new DefaultProxyBranch(branchId, target, request, priority, weight);
        }
    }

    /**
     * Note that this class is NOT static and shouldn't be.
     */
    private class ProxyBuilder implements Proxy.Builder {

        private final String friendlyName;
        private final List<ProxyBranchBuilder> branches = new ArrayList<>(2);
        private final SipRequest request;

        private ProxyBuilder(final String friendlyName, final SipRequest request) {
            this.friendlyName = friendlyName;
            this.request = request;
        }

        @Override
        public Proxy.Builder withParallelForking() {
            return null;
        }

        @Override
        public Proxy.Builder withSerialForking() {
            return null;
        }

        @Override
        public Proxy.Builder withSupervision() {
            return null;
        }

        @Override
        public Proxy.Builder withNoSupervision() {
            return null;
        }

        @Override
        public Proxy.Builder withRecordRoute(final Address address) {
            return null;
        }

        @Override
        public Proxy.Builder onBranchFailure(final BiConsumer<ProxyBranch, Event> consumer) {
            return null;
        }

        @Override
        public ProxyBranch.Builder withBranch(final SipURI target) {
            PreConditions.assertNotNull(target, "The proxy target cannot be null");
            final ProxyBranchBuilder branchBuilder = new ProxyBranchBuilder(target, request);
            branches.add(branchBuilder);
            return branchBuilder;
        }

        @Override
        public Proxy build() {
            // TODO: we probably want to sort them by priority and weight
            // before giving the branches to the proxy.
            final List<DefaultProxyBranch> branches = new ArrayList<>(this.branches.size());
            this.branches.forEach(b -> branches.add((DefaultProxyBranch)b.build()));

            final DefaultProxy proxy = new DefaultProxy(friendlyName, request, branches);
            registerProxy(proxy);
            return proxy;
        }
    }

    /**
     * Note that this class is NOT static and shouldn't be.
     */
    private class UABuilder implements UA.Builder {

        private final String friendlyName;
        private URI target;
        private DefaultSipRequestEvent request;

        private UABuilder(final String friendlyName) {
            this.friendlyName = friendlyName;
        }

        @Override
        public UA.Builder withTarget(final URI target) {
            this.target = target;
            return this;
        }

        @Override
        public UA.Builder withRequest(final SipRequestEvent request) {
            this.request = (DefaultSipRequestEvent) request;
            return this;
        }

        @Override
        public UA build() {
//            PreConditions.assertArgument(request != null && target == null, "Must set request or target");
//            PreConditions.assertArgument(request == null && target != null, "Must not set both request and target");

            final DefaultUA ua = new DefaultUA(tu, friendlyName, request, target);
            registerUA(ua);
            return ua;
        }
    }

    /**
     * Note that this class is NOT static and shouldn't be.
     */
    private class B2BUABuilder implements B2BUA.Builder {

        private final String friendlyName;
        private DefaultUA uaA;
        private DefaultUA uaB;

        private B2BUABuilder(final String friendlyName) {
            this.friendlyName = friendlyName;
        }

        @Override
        public B2BUA.Builder withA(final UA uaA) {
            this.uaA = (DefaultUA) uaA;
            return this;
        }

        @Override
        public B2BUA.Builder withB(final UA uaB) {
            this.uaB = (DefaultUA) uaB;
            return this;
        }

        @Override
        public B2BUA build() {
            final DefaultB2BUA b2bua = new DefaultB2BUA(friendlyName, uaA, uaB);
            registerB2BUA(b2bua);
            return b2bua;
        }
    }
}
