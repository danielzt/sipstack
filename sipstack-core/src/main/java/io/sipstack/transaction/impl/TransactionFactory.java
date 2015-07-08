package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipRequest;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transport.Flow;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionFactory {

    TransactionHolder createInviteServerTransaction(TransactionId id,
                                                    Flow flow,
                                                    SipRequest request,
                                                    TransactionLayerConfiguration config);

    TransactionHolder createInviteClientTransaction(TransactionId id,
                                                    Flow flow,
                                                    SipRequest request,
                                                    TransactionLayerConfiguration config);

    TransactionHolder createNonInviteServerTransaction(TransactionId id,
                                                       Flow flow,
                                                       SipRequest request,
                                                       TransactionLayerConfiguration config);
}
