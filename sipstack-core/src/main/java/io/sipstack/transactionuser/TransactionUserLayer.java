package io.sipstack.transactionuser;

import io.pkts.packet.sip.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionUserLayer {

    Dialog findOrCreateDialog(SipMessage message);
}
