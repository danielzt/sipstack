package io.sipstack.transactionuser;

import java.util.function.Consumer;

import io.pkts.packet.sip.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionUserLayer {

    Dialog findOrCreateDialog(SipMessage message);
}
