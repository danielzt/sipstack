package io.sipstack.transactionuser;

import java.util.function.Consumer;

import io.pkts.packet.sip.SipRequest;
import io.sipstack.transaction.Transaction;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionUserLayer {
    Dialog createDialog(Consumer<DialogEvent> consumer, Transaction tx, SipRequest request);
    Dialog createDialog(Consumer<DialogEvent> consumer, SipRequest request);
}
