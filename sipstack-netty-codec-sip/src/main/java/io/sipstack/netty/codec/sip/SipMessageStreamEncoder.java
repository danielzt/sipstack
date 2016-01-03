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
        if (msg.isSipMessageIOEvent()) {
            // TODO: dont do this. It will copy the buffer again...
            final byte[] data = msg.toSipMessageIOEvent().message().toBuffer().getArray();
            out.writeBytes(data);
            // ctx.flush();
        }
    }
}
