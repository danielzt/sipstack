/**
 * 
 */
package io.sipstack.netty.codec.sip.event;

import io.netty.channel.ChannelPipeline;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.header.ViaHeader;
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
     * Two SipMessageEvents are considered equal if they contain similar sip messages.
     * Similar because comparing the full sip message is expensive so we only care
     * about both being requests/responses, their method, cseq, transaction, call-id.
     *
     * @param other
     * @return
     */
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        try {
            final SipMessage otherMsg = ((SipMessageEvent)other).msg;
            if (msg.isRequest() != otherMsg.isRequest() && msg.getMethod().equals(otherMsg.getMethod())) {
                if (!msg.getCallIDHeader().equals(otherMsg.getCallIDHeader())) {
                    return false;
                }

                final ViaHeader via1 = msg.getViaHeader();
                final ViaHeader via2 = otherMsg.getViaHeader();
                return via1.getBranch().equals(via2.getBranch());
            } else {
                return false;
            }

        } catch (final NullPointerException | ClassCastException e) {
            return false;
        }
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
