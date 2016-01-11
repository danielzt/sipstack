/**
 * 
 */
package io.sipstack.netty.codec.sip;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.Transport;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.impl.SipParser;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public abstract class AbstractConnection implements Connection {

    private final ConnectionId id;
    private final Channel channel;
    private final InetSocketAddress remote;
    private final Optional<SipURI> vipAddress;
    private final static AttributeKey<Object> key = AttributeKey.newInstance("generic_object");

    /*
     * protected AbstractConnection(final ChannelHandlerContext ctx, final InetSocketAddress remote)
     * { this.ctx = ctx; this.channel = null; this.remote = remote; }
     */

    protected AbstractConnection(final Transport transport,
                                 final Channel channel,
                                 final InetSocketAddress remote,
                                 final Optional<SipURI> vipAddress) {
        this.id = ConnectionId.create(transport, (InetSocketAddress)channel.localAddress(), remote);
        this.channel = channel;
        this.remote = remote;
        this.vipAddress = vipAddress == null ? Optional.empty() : vipAddress;
    }

    protected AbstractConnection(final Transport transport, final Channel channel, final InetSocketAddress remote) {
        this(transport, channel, remote, null);
    }

    public Optional<SipURI> getVipAddress() {
        return vipAddress;
    }

    public final void storeObject(final Object o) {
        this.channel.attr(key).set(o);
    }

    public final Optional<Object> fetchObject() {
        return Optional.ofNullable(this.channel.attr(key).get());
    }

    protected Channel channel() {
        return this.channel;
    }

    @Override
    public Transport getTransport() {
        return this.id.getProtocol();
    }

    @Override
    public ConnectionId id() {
        return this.id;
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    @Override
    public void close() {
        // TODO: do we need to do more?
        channel.close();
    }
    @Override
    public byte[] getRawRemoteIpAddress() {
        return this.remote.getAddress().getAddress();
    }

    @Override
    public byte[] getRawLocalIpAddress() {
        final SocketAddress local = this.channel.localAddress();
        final InetAddress address = ((InetSocketAddress) local).getAddress();
        return address.getAddress();
    }

    @Override
    public final InetSocketAddress getLocalAddress() {
        return (InetSocketAddress)this.channel.localAddress();
    }

    @Override
    public final String getLocalIpAddress() {
        final SocketAddress local = this.channel.localAddress();
        return ((InetSocketAddress) local).getAddress().getHostAddress();
    }

    @Override
    public final Buffer getLocalIpAddressAsBuffer() {
        return Buffers.wrap(getLocalIpAddress());
    }

    @Override
    public final InetSocketAddress getRemoteAddress() {
        return this.remote;
    }

    @Override
    public final String getRemoteIpAddress() {
        return this.remote.getAddress().getHostAddress();
    }

    @Override
    public final Buffer getRemoteIpAddressAsBuffer() {
        return Buffers.wrap(getRemoteIpAddress());
    }

    @Override
    public int getLocalPort() {
        final SocketAddress local = this.channel.localAddress();
        return ((InetSocketAddress) local).getPort();
    }

    @Override
    public int getRemotePort() {
        return this.remote.getPort();
    }

    @Override
    public boolean isUDP() {
        return false;
    }

    @Override
    public boolean isTCP() {
        return false;
    }

    @Override
    public boolean isTLS() {
        return false;
    }

    @Override
    public boolean isSCTP() {
        return false;
    }

    @Override
    public boolean isWS() {
        return false;
    }

    // protected ChannelHandlerContext getContext() {
    // return this.ctx;
    // }

    /**
     * All {@link Connection}s needs to convert the msg to a {@link ByteBuf}
     * before writing it to the {@link ChannelHandlerContext}.
     * 
     * @param msg
     *            the {@link SipMessage} to convert.
     * @return the resulting {@link ByteBuf}
     */
    protected ByteBuf toByteBuf(final SipMessage msg) {
        try {
            final Buffer b = msg.toBuffer();
            final int capacity = b.capacity() + 2;
            final ByteBuf buffer = this.channel.alloc().buffer(capacity, capacity);

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
