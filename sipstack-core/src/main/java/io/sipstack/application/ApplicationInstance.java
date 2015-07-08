package io.sipstack.application;

import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipParseException;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.transactionuser.B2BUA;
import io.sipstack.transactionuser.Proxy;
import io.sipstack.transactionuser.TransactionUserEvent;
import io.sipstack.transactionuser.UA;

import java.io.IOException;

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

    public void onRequest(final SipRequest request) {
        // do nothing by default
    }

    public void onResponse(final SipResponse response) {
        // do nothing by default
    }
}
