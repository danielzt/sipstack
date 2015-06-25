package io.sipstack.application;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.InboundOutboundHandlerAdapter;
import io.sipstack.netty.codec.sip.event.SipMessageEvent;

/**
 * This is the adaptor between Netty and the layer facing the actual applications as
 * implemented by users of sipstack. There are different adapters to present
 * different interfaces such as a JSR289/359:ish adapter etc.
 *
 * @author jonas@jonasborjesson.com
 */
public class ApplicationHandler extends InboundOutboundHandlerAdapter {

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        final SipMessageEvent sipMessageEvent = (SipMessageEvent)msg;
        final Connection connection = sipMessageEvent.connection();
        final SipMessage sip = sipMessageEvent.message();
        // System.err.println("Application: " + sip);
        if (!sip.isAck()) {
            connection.send(sip.createResponse(200));
        }

    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        // System.out.println("Application.write");
        ctx.write(msg, promise);
    }

}
