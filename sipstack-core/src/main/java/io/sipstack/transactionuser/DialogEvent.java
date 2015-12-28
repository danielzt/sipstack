package io.sipstack.transactionuser;

/**
 * @author ajansson@twilio.com
 */
public interface DialogEvent {
    Dialog dialog();

    TransactionEvent transaction();
}
