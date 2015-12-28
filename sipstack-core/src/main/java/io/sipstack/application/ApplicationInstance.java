package io.sipstack.application;

import java.io.IOException;

import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipParseException;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.transactionuser.Proxy;

/**
 * @author jonas@jonasborjesson.com
 */
public abstract class ApplicationInstance {


    static final ThreadLocal<ApplicationContext> _ctx = new ThreadLocal<>();

    private final Buffer id;

    public ApplicationInstance(final Buffer id) {
        this.id = id;
    }

    public final Buffer id() {
        return id;
    }

    private ApplicationContext ctx() {
        return _ctx.get();
    }

    public final UA.Builder uaWithFriendlyName(final String friendlyName) {
        return ctx().uaWithFriendlyName(friendlyName);
    }

    public final B2BUA.Builder b2buaWithFriendlyName(final String friendlyName) {
        return ctx().b2buaWithFriendlyName(friendlyName);
    }

    public final Proxy.Builder proxy(final SipURI to) {
        return ctx().proxy(to);
    }

    public final Proxy.Builder proxy(final String to) throws SipParseException {
        try {
            return proxy(SipURI.frame(Buffers.wrap(to)));
        } catch (final IOException e) {
            throw new SipParseException("Issue when creating buffer to hold the sip message");
        }
    }

    public final Proxy.Builder proxyWithFriendlyName(final String friendlyName) {
        return ctx().proxyWithFriendlyName(friendlyName);
    }

    /**
     * Default event handler.
     * @param request Event
     */
    public void onRequest(final SipRequestEvent request) {
        // do nothing by default
    }

    /**
     * Default event handler.
     * @param response Event
     */
    public void onResponse(final SipResponseEvent response) {
        // do nothing by default
    }
}
