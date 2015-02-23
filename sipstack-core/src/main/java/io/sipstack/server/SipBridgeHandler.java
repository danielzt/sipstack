/**
 * 
 */
package io.sipstack.server;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.sipstack.actor.ActorSystem;
import io.sipstack.netty.codec.sip.SipMessageEvent;

/**
 * @author jonas@jonasborjesson.com
 */
@Sharable
public class SipBridgeHandler extends SimpleChannelInboundHandler<SipMessageEvent> {
    
    private final ActorSystem system;

    /**
     * 
     */
    public SipBridgeHandler(final ActorSystem system) {
        this.system = system;
    }


    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageEvent msg) throws Exception {
        this.system.receive(msg);
    }

}
