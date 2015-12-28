/**
 * 
 */
package io.sipstack.netty.codec.sip;

import gov.nist.javax.sip.address.SipUri;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.impl.SipInitialLine;
import io.sipstack.netty.codec.sip.event.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * @author jonas
 * 
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
        reset();
    }

    @Override
    public boolean isSingleDecode() {
        return true;
    }

    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
        System.err.println("registered");
        ctx.fireUserEventTriggered(create(ctx, ConnectionOpenedIOEvent::create));
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        System.err.println("un-registered");
        ctx.fireUserEventTriggered(create(ctx, ConnectionClosedIOEvent::create));
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        System.err.println("active");
        ctx.fireUserEventTriggered(create(ctx, ConnectionActiveIOEvent::create));
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        System.err.println("in-active");
        ctx.fireUserEventTriggered(create(ctx, ConnectionInactiveIOEvent::create));
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {
        // just consume the event
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
        try {
            while (!this.message.isComplete() && buffer.isReadable()) {
                final byte b = buffer.readByte();
                this.message.write(b);
            }
        } catch (final MaxMessageSizeExceededException e) {
            dropConnection(ctx, e.getMessage());
            // TODO: mark this connection as dead since the future
            // for closing this decoder may take a while to actually
            // do its job
        } catch (final IOException e) {
            e.printStackTrace();
        }

        if (this.message.isComplete()) {
            final long arrivalTime = this.clock.getCurrentTimeMillis();
            final Channel channel = ctx.channel();
            final Connection connection = new TcpConnection(channel, (InetSocketAddress) channel.remoteAddress(), vipAddress);
            final SipMessageIOEvent msg = toSipMessageIOEvent(connection, this.message);
            out.add(msg);
            reset();
        }
    }

    private SipMessageIOEvent toSipMessageIOEvent(final Connection connection, final RawMessage raw) {
        if (true) {
            throw new RuntimeException("Need to redo this now with the immutable way of doing things");
        }
        final SipInitialLine initialLine = SipInitialLine.parse(raw.getInitialLine());
        final Buffer headers = raw.getHeaders();
        final Buffer payload = raw.getPayload();
        if (initialLine.isRequestLine()) {
            // final SipRequest request = new SipRequestImpl((SipRequestLine) initialLine, headers, payload);
            // return IOEvent.create(connection, request);
        } else {
            // final SipResponse response = new SipResponseImpl((SipResponseLine) initialLine, headers, payload);
            // return IOEvent.create(connection, response);
        }
        return null;
    }

    private void dropConnection(final ChannelHandlerContext ctx, final String reason) {
    }

    private void reset() {
        this.message = new RawMessage(MAX_ALLOWED_INITIAL_LINE_SIZE, MAX_ALLOWED_HEADERS_SIZE,
                MAX_ALLOWED_CONTENT_LENGTH);
    }

}
