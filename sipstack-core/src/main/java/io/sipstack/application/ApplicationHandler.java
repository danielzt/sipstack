package io.sipstack.application;

import io.netty.channel.ChannelHandlerContext;
import io.sipstack.netty.codec.sip.InboundOutboundHandlerAdapter;

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
        System.err.println("Application: " + msg);
    }

}
