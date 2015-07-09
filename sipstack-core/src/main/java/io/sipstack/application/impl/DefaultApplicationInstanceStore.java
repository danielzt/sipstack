package io.sipstack.application.impl;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.application.ApplicationController;
import io.sipstack.application.ApplicationInstance;
import io.sipstack.application.ApplicationInstanceCreator;
import io.sipstack.application.ApplicationInstanceStore;
import io.sipstack.transactionuser.TransactionUserLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultApplicationInstanceStore implements ApplicationInstanceStore {

    private final TransactionUserLayer tu;
    private final Map<Buffer, ApplicationInstance> instances;
    private final Map<Buffer, InternalApplicationContext> contexts;
    private final ApplicationInstanceCreator creator;

    public DefaultApplicationInstanceStore(final TransactionUserLayer tu, final ApplicationInstanceCreator creator) {
        this.tu = tu;
        this.creator = creator;
        instances = new ConcurrentHashMap<>(100000);
        contexts = new ConcurrentHashMap<>(100000);
    }

    @Override
    public ApplicationInstance ensureApplication(final SipMessage msg) {
        final Buffer id = creator.getId(msg);
        return instances.computeIfAbsent(id, obj -> creator.createInstance(id, msg));
    }

    @Override
    public InternalApplicationContext ensureApplicationContext(final Buffer appId) {
        return contexts.computeIfAbsent(appId, obj -> new DefaultApplicationContext(tu));
    }

    @Override
    public ApplicationInstance get(final Buffer id) {
        return instances.get(id);
    }

    @Override
    public void remove(final String id) {
        instances.remove(id);
    }
}
