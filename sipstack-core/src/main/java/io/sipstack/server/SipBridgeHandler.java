/**
 * 
 */
package io.sipstack.server;

import io.hektor.core.ActorRef;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.sipstack.netty.codec.sip.SipMessageEvent;

/**
 * @author jonas@jonasborjesson.com
 */
@Sharable
public class SipBridgeHandler extends SimpleChannelInboundHandler<SipMessageEvent> {

    private final ActorRef actor;

    /**
     * 
     */
    public SipBridgeHandler(final ActorRef actor) {
        this.actor = actor;
    }


    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageEvent msg) throws Exception {
        actor.tellAnonymously(msg);
    }

}
