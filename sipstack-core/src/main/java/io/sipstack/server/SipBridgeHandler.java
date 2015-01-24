/**
 * 
 */
package io.sipstack.server;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.sipstack.netty.codec.sip.SipMessageEvent;
import akka.actor.ActorRef;

/**
 * @author jonas@jonasborjesson.com
 */
@Sharable
public class SipBridgeHandler extends SimpleChannelInboundHandler<SipMessageEvent> {

    private final ActorRef akka;


    /**
     * 
     */
    public SipBridgeHandler(final ActorRef akka) {
        this.akka = akka;
    }


    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageEvent msg) throws Exception {
        this.akka.tell(msg, ActorRef.noSender());
    }

}
