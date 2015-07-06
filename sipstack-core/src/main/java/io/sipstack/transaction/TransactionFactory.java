package io.sipstack.transaction;

import io.pkts.packet.sip.SipRequest;
import io.sipstack.config.TransactionLayerConfiguration;

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
