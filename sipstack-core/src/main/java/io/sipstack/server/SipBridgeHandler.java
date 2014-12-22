/**
 * 
 */
package io.sipstack.server;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.sipstack.netty.codec.sip.SipMessageEvent;

/**
 * @author jonas@jonasborjesson.com
 */
@Sharable
public class SipBridgeHandler extends SimpleChannelInboundHandler<SipMessageEvent> {

    /**
     * 
     */
    public SipBridgeHandler() {
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageEvent msg) throws Exception {
        System.err.println("got message: " + msg.getMessage());
    }

}
