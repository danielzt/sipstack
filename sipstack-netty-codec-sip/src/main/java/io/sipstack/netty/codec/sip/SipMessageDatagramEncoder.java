package io.sipstack.netty.codec.sip;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.event.IOEvent;

import java.util.List;

/**
 * Simple encoder that takes a {@link IOEvent} and turns it into a {@link DatagramPacket}.
 *
 * @author jonas@jonasborjesson.com
 */
public class SipMessageDatagramEncoder extends MessageToMessageEncoder<IOEvent> {

    @Override
    protected void encode(final ChannelHandlerContext ctx, final IOEvent event, final List<Object> out) throws Exception {
        final Connection connection = event.connection();

        if (event.isSipMessageIOEvent()) {
            final SipMessage msg = event.toSipMessageIOEvent().message();
            final DatagramPacket pkt = new DatagramPacket(Utils.toByteBuf(ctx.channel(), msg), connection.getRemoteAddress());
            out.add(pkt);
        } else if (event.isSipMessageBuilderIOEvent()) {
            System.err.println("Builder event, building...");
            final SipMessage msg = event.toSipMessageBuilderIOEvent().getBuilder().build();
            final DatagramPacket pkt = new DatagramPacket(Utils.toByteBuf(ctx.channel(), msg), connection.getRemoteAddress());
            out.add(pkt);
        }
    }

}
