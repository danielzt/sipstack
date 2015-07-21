/**
 * 
 */
package io.sipstack.example.netty.sip.uas;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.netty.codec.sip.event.impl.SipMessageIOEventImpl;

/**
 * A super simple UAS implementation.
 * 
 * @author jonas@jonasborjesson.com
 */
@Sharable
public final class UASHandler extends SimpleChannelInboundHandler<SipMessageIOEventImpl> {

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageIOEventImpl event) throws Exception {
        final SipMessage msg = event.message();

        // just consume the ACK
        if (msg.isAck()) {
            return;
        }

        // for all requests, just generate a 200 OK response.
        if (msg.isRequest()) {
            final SipResponse response = msg.createResponse(200);
            event.connection().send(response);
        }
    }

}
