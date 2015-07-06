package io.sipstack.transport;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.event.Event;
import io.sipstack.netty.codec.sip.ConnectionId;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Flow {

    /**
     * Ok, perhaps this should be called FlowId but it is
     * exactly the same thing so it felt silly...
     *
     * @return
     */
    ConnectionId id();

    void write(Event event);

    void write(SipMessage msg);
}
