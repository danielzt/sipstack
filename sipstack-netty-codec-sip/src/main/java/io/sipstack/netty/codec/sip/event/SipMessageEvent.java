/**
 * 
 */
package io.sipstack.netty.codec.sip.event;

import io.netty.channel.ChannelPipeline;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.Connection;

/**
 * Whenever a {@link SipMessage} is received it will be framed and a
 * {@link SipMessageEvent} will be created and passed up through the
 * {@link ChannelPipeline}. The main reason for this object is the need to
 * encapsulate stream based and datagram based connections in Netty 4 (worked
 * differently in Netty 3) as well as to provide a time stamp for when the
 * message was received on the socket (or rather when it was framed)
 * 
 * @author jonas@jonasborjesson.com
 */
public class SipMessageEvent extends Event {

    private final Connection connection;
    private final SipMessage msg;


    /**
     *
     */
    public SipMessageEvent(final Connection connection, final SipMessage msg, final long arrivalTime) {
        super(arrivalTime);
        this.connection = connection;
        this.msg = msg;
    }


    /**
     * The {@link Connection} over which this {@link SipMessage} was received.
     *
     * @return
     */
    public Connection connection() {
        return this.connection;
    }

    /**
     * The framed {@link SipMessage}.
     *
     * @return
     */
    public SipMessage message() {
        return this.msg;
    }

    @Override
    public final boolean isSipMessageEvent() {
        return true;
    }

    @Override
    public final SipMessageEvent toSipMessageEvent() {
        return this;
    }

}
