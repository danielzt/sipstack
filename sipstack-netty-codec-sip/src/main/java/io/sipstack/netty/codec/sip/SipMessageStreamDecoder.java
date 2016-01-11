/**
 * 
 */
package io.sipstack.netty.codec.sip;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.impl.SipMessageStreamBuilder;
import io.pkts.packet.sip.impl.SipMessageStreamBuilder.DefaultConfiguration;
import io.sipstack.netty.codec.sip.event.*;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
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
    public static final int MAX_ALLOWED_HEADERS_SIZE = 2048;
    // public static final int MAX_ALLOWED_HEADERS_SIZE = 8192;

    public static final int MAX_ALLOWED_CONTENT_LENGTH = 1024;

    private final Clock clock;

    private final Optional<SipURI> vipAddress;

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
        this.vipAddress = Optional.ofNullable(vipAddress);
        final DefaultConfiguration config = new DefaultConfiguration();
        config.setMaxAllowedHeadersSize(2048);
        config.setMaxAllowedContentLength(1024);
        messageBuilder = new SipMessageStreamBuilder(config);
    }

    @Override
    public boolean isSingleDecode() {
        return true;
    }

    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().localAddress() != null) {
            ctx.fireUserEventTriggered(create(ctx, ConnectionOpenedIOEvent::create));
        }
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().localAddress() != null) {
            ctx.fireUserEventTriggered(create(ctx, ConnectionClosedIOEvent::create));
        }
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().localAddress() != null) {
            final ConnectionIOEvent event = create(ctx, ConnectionActiveIOEvent::create);
            ctx.fireUserEventTriggered(event);
        }
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().localAddress() != null) {
            final ConnectionIOEvent event = create(ctx, ConnectionInactiveIOEvent::create);
            ctx.fireUserEventTriggered(event);
        }
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
    }

    private void dropConnection(final ChannelHandlerContext ctx, final String reason) {
    }

}
