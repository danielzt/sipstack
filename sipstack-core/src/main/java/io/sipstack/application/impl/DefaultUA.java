package io.sipstack.application.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.address.URI;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.application.UA;
import io.sipstack.transactionuser.Dialog;
import io.sipstack.transactionuser.DialogEvent;
import io.sipstack.transactionuser.TransactionUserLayer;

/**
 * @author ajansson@twilio.com
 */
public class DefaultUA implements UA, Consumer<DialogEvent> {
    private final Logger logger = LoggerFactory.getLogger(DefaultUA.class);

    private final TransactionUserLayer tu;
    private final String friendlyName;
    private final URI target;
    private final List<Consumer<SipMessage>> handlers = new ArrayList<>(2);
    private final DefaultSipRequestEvent request;
    private volatile Dialog dialog;

    public DefaultUA(final TransactionUserLayer tu, final String friendlyName, final DefaultSipRequestEvent request,
            final URI target) {
        this.tu = tu;
        this.friendlyName = friendlyName;
        this.request = request;
        this.target = target;
        if (request != null) {
            dialog = tu.createDialog(this, request.transaction(), request.message());
        }
    }

    public String friendlyName() {
        return friendlyName;
    }

    public URI getTarget() {
        return target;
    }

    @Override
    public void send(SipRequest.Builder message) {
        assertDialog(message.build()).send(message);
    }

    public void send(SipResponse message) {
        assertDialog(message).send(message);
    }

    private Dialog assertDialog(final SipMessage message) {
        if (dialog == null && message.isRequest()) {
            dialog = tu.createDialog(this, message.toRequest());
        }
        if (dialog == null) {
            throw new IllegalStateException("No dialog available");
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
    public SipRequest.Builder createBye() {
        PreConditions.assertArgument(dialog != null, "No dialog created");
        return dialog.createBye();
    }

    @Override
    public void accept(final DialogEvent event) {
        final SipMessage message = event.transaction().message();
        handlers.forEach(h -> h.accept(message));
    }

    public SipRequest getRequest() {
        return request.message();
    }
}
