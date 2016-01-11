package io.sipstack.netty.codec.sip;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.pkts.buffer.Buffer;
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

            // Note: you don't want to do msgBuffer.getArray() since that will create
            // a copy and we are trying to avoid that. However, accessing the raw array
            // also means that you have to pay attention to which portion of that data
            // is actually visible to the buffer.
            final Buffer msgBuffer = msg.toSipMessageIOEvent().message().toBuffer();
            final byte[] rawData = msgBuffer.getRawArray();
            out.writeBytes(rawData, msgBuffer.getLowerBoundary() + msgBuffer.getReaderIndex(), msgBuffer.getReadableBytes());
        }
    }
}
