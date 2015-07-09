package io.sipstack.transactionuser.impl;

import io.sipstack.transactionuser.Dialog;
import io.sipstack.transactionuser.DialogEvent;
import io.sipstack.transactionuser.TransactionEvent;

/**
 * @author ajansson@twilio.com
 */
public class DefaultDialogEvent implements DialogEvent {
    private final Dialog dialog;
    private final TransactionEvent tx;

    public DefaultDialogEvent(final Dialog dialog, final TransactionEvent tx) {
        this.dialog = dialog;
        this.tx = tx;
    }

    @Override
    public Dialog dialog() {
        return dialog;
    }

    @Override
    public TransactionEvent transaction() {
        return tx;
    }
}
