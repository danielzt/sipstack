/**
 * 
 */
package io.sipstack.netty.codec.sip;

import gov.nist.javax.sip.address.SipUri;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.impl.SipInitialLine;
import io.pkts.packet.sip.impl.SipMessageStreamBuilder;
import io.pkts.packet.sip.impl.SipMessageStreamBuilder.Configuration;
import io.pkts.packet.sip.impl.SipMessageStreamBuilder.DefaultConfiguration;
import io.sipstack.netty.codec.sip.event.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * NOTE! This is NOT a sharable class because it is very much stateful and
 * as such we need a new one for every new pipeline.
 *
 * @author jonas@jonasborjesson.com
 */
public class SipMessageStreamDecoder extends ByteToMessageDecoder {

    /**
     * The maximum allowed initial line. If we pass this threshold we will drop
     * the message and close down the connection (if we are using a connection
     * oriented protocol ie)
     */
    public static final int MAX_ALLOWED_INITIAL_LINE_SIZE = 1024;

    /**
     * The maximum allowed size of ALL headers combined (in bytes).
     */
    public static final int MAX_ALLOWED_HEADERS_SIZE = 8192;

    public static final int MAX_ALLOWED_CONTENT_LENGTH = 2048;

    private final Clock clock;

    private final SipURI vipAddress;

    /**
     * Contains the raw framed message.
     */
    private RawMessage message;

    private SipMessageStreamBuilder messageBuilder;

    public SipMessageStreamDecoder() {
        this(new SystemClock(), null);
    }

    public SipMessageStreamDecoder(final Clock clock) {
        this(clock, null);
    }
    /**
     * 
     */
    public SipMessageStreamDecoder(final Clock clock, final SipURI vipAddress) {
        this.clock = clock;
        this.vipAddress = vipAddress;
        final Configuration config = new DefaultConfiguration();
        messageBuilder = new SipMessageStreamBuilder(config);
    }

    @Override
    public boolean isSingleDecode() {
        return true;
    }

    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
        ctx.fireUserEventTriggered(create(ctx, ConnectionOpenedIOEvent::create));
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        ctx.fireUserEventTriggered(create(ctx, ConnectionClosedIOEvent::create));
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        ctx.fireUserEventTriggered(create(ctx, ConnectionActiveIOEvent::create));
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        ctx.fireUserEventTriggered(create(ctx, ConnectionInactiveIOEvent::create));
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {
        // just consume the event
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
        // System.err.println("Done reading in the UDP decoder");
        // ctx.flush();
    }

    private ConnectionIOEvent create(final ChannelHandlerContext ctx, final BiFunction<Connection, Long, ConnectionIOEvent> f) {
        final Channel channel = ctx.channel();
        final Connection connection = new TcpConnection(channel, (InetSocketAddress) channel.remoteAddress(), vipAddress);
        final Long arrivalTime = clock.getCurrentTimeMillis();
        return f.apply(connection, arrivalTime);
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf buffer, final List<Object> out)
            throws Exception {

        // final long ts = System.currentTimeMillis();
        // TODO: if you push a tonnes of traffic over a single TCP connection
        // it may be that we actually receive buffers that are larger than we
        // have actually allocated in our message builder. What we should do is
        // to only fill up max data
        while (buffer.isReadable()) {

            final int availableBytes = buffer.readableBytes();
            final int writableBytes = messageBuilder.getWritableBytes();
            final int toWrite = Math.min(availableBytes, writableBytes);
            final byte[] data = new byte[toWrite];
            buffer.readBytes(data);

            // final int bytesToCopy = Math.min(availableBytes, messageBuilder.getWritableBytes());
            // buffer.readBytes(array, messageBuilder.getWriterIndex(), bytesToCopy);

            // if (messageBuilder.processNewData(bytesToCopy)) {
            if (messageBuilder.process(data)) {
                final SipMessage sipMessage = messageBuilder.build();
                final long arrivalTime = this.clock.getCurrentTimeMillis();
                final Channel channel = ctx.channel();
                final Connection connection = new TcpConnection(channel, (InetSocketAddress) channel.remoteAddress(), vipAddress);
                final SipMessageIOEvent msg = IOEvent.create(connection, sipMessage);
                out.add(msg);

                while (messageBuilder.hasUnprocessData() && messageBuilder.process()) {
                    out.add(IOEvent.create(connection, messageBuilder.build()));
                }
            }
        }

        // buffer.readerIndex(buffer.readerIndex() + buf.getReaderIndex());

        // } catch (final MaxMessageSizeExceededException e) {
            // dropConnection(ctx, e.getMessage());
            // TODO: mark this connection as dead since the future
            // for closing this decoder may take a while to actually
            // do its job
        // } catch (final IOException e) {
            // e.printStackTrace();
        // }

        /*
        if (this.message.isComplete()) {
            final long arrivalTime = this.clock.getCurrentTimeMillis();
            final Channel channel = ctx.channel();
            final Connection connection = new TcpConnection(channel, (InetSocketAddress) channel.remoteAddress(), vipAddress);
            final SipMessageIOEvent msg = toSipMessageIOEvent(connection, this.message);
            out.add(msg);
            reset();
        }
        */
    }

    private void dropConnection(final ChannelHandlerContext ctx, final String reason) {
    }

}
