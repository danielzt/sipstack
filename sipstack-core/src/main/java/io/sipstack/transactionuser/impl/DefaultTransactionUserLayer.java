package io.sipstack.transactionuser.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionUser;
import io.sipstack.transaction.Transactions;
import io.sipstack.transactionuser.Dialog;
import io.sipstack.transactionuser.DialogEvent;
import io.sipstack.transactionuser.TransactionEvent;
import io.sipstack.transactionuser.TransactionUserLayer;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultTransactionUserLayer implements TransactionUserLayer, TransactionUser {

    private Transactions transactions;
    private final Consumer<TransactionEvent> consumer;
    private Map<Buffer, Dialogs> dialogs = new ConcurrentHashMap<>();

    public DefaultTransactionUserLayer(final Consumer<TransactionEvent> consumer) {
        this.consumer = consumer;
    }

    public void start(final Transactions transactionLayer) {
        this.transactions = transactionLayer;
    }

    @Override
    public Dialog createDialog(final Consumer<DialogEvent> consumer, final Transaction tx, final SipRequest request) {
        final boolean isUpstream = true;
        final Buffer key = Dialogs.getDialogKey(request, isUpstream);
        final Dialogs dialog = new Dialogs(consumer, transactions, tx, request, isUpstream);
        this.dialogs.put(key, dialog);
        return dialog.getDialog(request);
    }

    @Override
    public Dialog createDialog(final Consumer<DialogEvent> consumer, final SipRequest request) {
        final boolean isUpstream = false;
        final Buffer key = Dialogs.getDialogKey(request, isUpstream);
        final Dialogs dialog = new Dialogs(consumer, transactions, null, request, isUpstream);
        this.dialogs.put(key, dialog);
        return dialog.getDialog(request);
    }

    private Dialogs findDialog(final SipMessage message) {
        final boolean isUac = true;
        final Buffer key = Dialogs.getDialogKey(message, isUac);
        return dialogs.get(key);
    }

    @Override
    public void onRequest(Transaction tx, SipRequest request) {
        final Dialogs dialog = findDialog(request);
        final TransactionEvent transactionEvent = new DefaultTransactionEvent(tx, request);
        if (dialog != null) {
            dialog.dispatchUpstream(transactionEvent);
        } else {
            consumer.accept(transactionEvent);
        }
    }

    @Override
    public void onResponse(Transaction tx, SipResponse response) {
        final Dialogs dialog = findDialog(response);
        final TransactionEvent transactionEvent = new DefaultTransactionEvent(tx, response);
        if (dialog != null) {
            dialog.dispatchUpstream(transactionEvent);
        } else {
            consumer.accept(transactionEvent);
        }
    }

    @Override
    public void onTransactionTerminated(Transaction transaction) {

    }

    @Override
    public void onIOException(Transaction transaction, SipMessage msg) {

    }
}
