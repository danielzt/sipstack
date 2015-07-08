package io.sipstack.transactionuser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.address.URI;
import io.sipstack.application.DefaultApplicationContext;

/**
 * @author ajansson@twilio.com
 */
public class DefaultUA implements UA, Consumer<SipMessage> {

    private final DefaultApplicationContext parent;
    private final String friendlyName;
    private final URI target;
    private final SipRequest request;
    private final List<Consumer<SipMessage>> handlers = new ArrayList<>(2);

    public DefaultUA(final DefaultApplicationContext parent, final String friendlyName, final SipRequest request, final URI target) {
        this.parent = parent;
        this.friendlyName = friendlyName;
        this.request = request;
        this.target = target;
    }

    public String friendlyName() {
        return friendlyName;
    }

    public SipRequest getRequest() {
        return request;
    }

    public URI getTarget() {
        return target;
    }

    @Override
    public void send(final SipMessage message) {
        parent.send(message);
    }

    @Override
    public void addHandler(final Consumer<SipMessage> handler) {
        handlers.add(handler);
    }

    @Override
    public void accept(final SipMessage sipMessage) {
        handlers.forEach(h -> h.accept(sipMessage));
    }
}
