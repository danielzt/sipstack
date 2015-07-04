package io.sipstack.netty.codec.sip.application;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultApplicationInstanceStore implements ApplicationInstanceStore {

    private final Map<Buffer, ApplicationInstance> instances;
    private final Map<Buffer, InternalApplicationContext> contexts;
    private final ApplicationInstanceCreator creator;

    public DefaultApplicationInstanceStore(final ApplicationInstanceCreator creator) {
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
        return contexts.computeIfAbsent(appId, obj -> new DefaultApplicationContext());
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
