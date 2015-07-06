/**
 * 
 */
package io.sipstack.netty.codec.sip;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.impl.SipParser;

import java.io.IOException;
import java.util.List;

/**
 * @author jonas
 *
 */
public class SipMessageEncoder extends MessageToMessageEncoder<SipMessageEvent> {

    // this is when we do MessageToByteEncoder
    protected void encode(final ChannelHandlerContext ctx, final SipMessage msg, final ByteBuf out) {
        System.err.println("SipMessageEncoder");
        try {
            final Buffer b = msg.toBuffer();
            for (int i = 0; i < b.getReadableBytes(); ++i) {
                out.writeByte(b.getByte(i));
            }
            out.writeByte(SipParser.CR);
            out.writeByte(SipParser.LF);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final SipMessageEvent msg, List<Object> out) throws Exception {
        final Connection connection = msg.connection();
        final SipMessage sip = msg.message();
        final DatagramPacket pkt = new DatagramPacket(toByteBuf(ctx.channel(), sip), connection.getRemoteAddress());
        out.add(pkt);
    }

    protected ByteBuf toByteBuf(final Channel channel, final SipMessage msg) {
        try {
            final Buffer b = msg.toBuffer();
            final int capacity = b.capacity() + 2;
            final ByteBuf buffer = channel.alloc().buffer(capacity, capacity);

            for (int i = 0; i < b.getReadableBytes(); ++i) {
                buffer.writeByte(b.getByte(i));
            }
            buffer.writeByte(SipParser.CR);
            buffer.writeByte(SipParser.LF);
            return buffer;
        } catch (final IOException e) {
            // shouldn't be possible since the underlying buffer
            // from the msg is backed by a byte-array.
            // TODO: do something appropriate other than this
            throw new RuntimeException("Unable to convert SipMessage to a ByteBuf due to IOException", e);
        }
    }
}
