package io.sipstack.netty.codec.sip;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.sipstack.netty.codec.sip.event.IOEvent;

/**
 * Simple encoder turning an {@link IOEvent} into a byte array.
 *
 * @author jonas@jonasborjesson.com
 */
public class SipMessageStreamEncoder extends MessageToByteEncoder<IOEvent> {

    @Override
    protected void encode(final ChannelHandlerContext ctx, final IOEvent msg, final ByteBuf out) throws Exception {
        System.err.println("yeah???");
        if (msg.isSipMessageIOEvent()) {
            out.writeBytes(msg.toSipMessageIOEvent().message().toBuffer().getArray());
        }
    }
}
