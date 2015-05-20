/**
 * 
 */
package io.sipstack.transaction.impl;

import io.hektor.core.Actor;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.event.Event;
import io.sipstack.event.IOWriteEvent;
import io.sipstack.transaction.TransactionId;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public class ServerTransactionActor implements Actor {

    private final TransactionId id;

    /**
     * @param id
     */
    public ServerTransactionActor(final TransactionId id) {
        this.id = id;
    }

    @Override
    public void onReceive(final Object msg) {

        final Event event = (Event)msg;
        if (event.isSipReadEvent()) {
            final SipMessage sip = (SipMessage)event.toIOEvent().getObject();
            if (!sip.isAck()) {
                final SipResponse response = sip.createResponse(200);
                sender().tell(IOWriteEvent.create(response), self());
            } else {
            }
        }
    }
}
