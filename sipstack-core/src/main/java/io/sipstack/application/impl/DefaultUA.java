package io.sipstack.application.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.address.URI;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.application.UA;
import io.sipstack.transactionuser.Dialog;
import io.sipstack.transactionuser.TransactionUserEvent;
import io.sipstack.transactionuser.TransactionUserLayer;

/**
 * @author ajansson@twilio.com
 */
public class DefaultUA implements UA, Consumer<TransactionUserEvent> {
    private final Logger logger = LoggerFactory.getLogger(DefaultUA.class);

    private final TransactionUserLayer tu;
    private final String friendlyName;
    private final URI target;
    private final List<Consumer<SipMessage>> handlers = new ArrayList<>(2);
    private final SipRequest request;
    private volatile Dialog dialog;

    public DefaultUA(final TransactionUserLayer tu, final String friendlyName, final SipRequest request,
            final URI target) {
        this.tu = tu;
        this.friendlyName = friendlyName;
        this.request = request;
        this.target = target;
        if (request != null) {
            assertDialog(request);
        }
    }

    public void setDialog(final Dialog dialog) {
        this.dialog = dialog;
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

        assertDialog(message).send(message);
    }

    private Dialog assertDialog(final SipMessage message) {
        if (dialog == null) {
            dialog = tu.findOrCreateDialog(message);
            dialog.setConsumer(this);
        }
        return dialog;
    }

    @Override
    public void addHandler(final Consumer<SipMessage> handler) {
        handlers.add(handler);
    }

    @Override
    public SipRequest.Builder createAck() {
        PreConditions.assertArgument(dialog != null, "No dialog created");
        return dialog.createAck();
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
