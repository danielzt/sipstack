package io.sipstack.transactionuser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.address.URI;
import io.sipstack.application.DefaultApplicationContext;

/**
 * @author ajansson@twilio.com
 */
public class DefaultUA implements UA, Consumer<TransactionUserEvent> {
    private final Logger logger = LoggerFactory.getLogger(DefaultUA.class);

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
        log(message, " -> ");
        parent.send(this, message);
    }

    @Override
    public void addHandler(final Consumer<SipMessage> handler) {
        handlers.add(handler);
    }

    @Override
    public void accept(final TransactionUserEvent event) {
        final SipMessage message = event.message();
        log(message, " <- ");
        handlers.forEach(h -> h.accept(event.message()));
    }

    private void log(final SipMessage message, final String direction) {
        final StringBuilder sb = new StringBuilder();
        sb.append(friendlyName).append(direction);
        if (message.isRequest()) {
            sb.append(message.getMethod());
        } else {
            sb.append(message.toResponse().getStatus());
        }
        logger.info(sb.toString());
    }
}
