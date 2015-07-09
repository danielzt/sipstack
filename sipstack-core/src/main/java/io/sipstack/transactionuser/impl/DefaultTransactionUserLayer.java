package io.sipstack.transactionuser.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionUser;
import io.sipstack.transaction.Transactions;
import io.sipstack.transactionuser.Dialog;
import io.sipstack.transactionuser.TransactionUserEvent;
import io.sipstack.transactionuser.TransactionUserLayer;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultTransactionUserLayer implements TransactionUserLayer, TransactionUser {

    private Transactions transactions;
    private final Consumer<TransactionUserEvent> consumer;
    private Map<String, Dialogs> dialogs = new ConcurrentHashMap<>();

    public DefaultTransactionUserLayer(final Consumer<TransactionUserEvent> consumer) {
        this.consumer = consumer;
    }

    public void start(final Transactions transactionLayer) {
        this.transactions = transactionLayer;
    }

    @Override
    public Dialog findOrCreateDialog(final SipMessage message) {
        return findOrCreateDialogs(message).process(null, message);
    }

    private Dialogs findOrCreateDialogs(final SipMessage message) {
        return dialogs.computeIfAbsent(getDialogKey(message), k -> {
            PreConditions.assertArgument(message.isRequest(), "Must be request");
            return new Dialogs(transactions, message.toRequest());
        });
    }

    /**
     * Gets the dialog key. Currently just uses call id.
     * @param message SIP message
     * @return Dialog key.
     */
    private static String getDialogKey(final SipMessage message) {
        return message.getCallIDHeader().getCallId().toString();
    }

    @Override
    public void onRequest(Transaction tx, SipRequest request) {
        final Dialogs dialogs = findOrCreateDialogs(request);
        final Dialog dialog = dialogs.process(tx, request);
        consumer.accept(new TransactionUserEvent(dialog, tx, request));
    }

    @Override
    public void onResponse(Transaction tx, SipResponse response) {
        final Dialogs dialogs = findOrCreateDialogs(response);
        final Dialog dialog = dialogs.process(tx, response);
        consumer.accept(new TransactionUserEvent(dialog, tx, response));
    }

    @Override
    public void onTransactionTerminated(Transaction transaction) {

    }

    @Override
    public void onIOException(Transaction transaction, SipMessage msg) {

    }
}
