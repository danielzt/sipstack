package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipRequest;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.transaction.TransactionId;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionFactory {

    TransactionHolder createInviteServerTransaction(TransactionId id,
                                                    SipRequest request,
                                                    TransactionLayerConfiguration config);

    TransactionHolder createNonInviteServerTransaction(TransactionId id,
                                                       SipRequest request,
                                                       TransactionLayerConfiguration config);
}
