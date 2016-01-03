/**
 * 
 */
package io.sipstack.netty.codec.sip;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import io.pkts.packet.sip.address.SipURI;

import java.net.InetSocketAddress;

/**
 *
 * @author jonas@jonasborjesson.com
 */
public final class TcpConnection extends AbstractConnection {


    public TcpConnection(final Channel channel, final InetSocketAddress remote, final SipURI vipAddress) {
        super(Transport.tcp, channel, remote, vipAddress);
    }

    public TcpConnection(final Channel channel, final InetSocketAddress remote) {
        super(Transport.tcp, channel, remote, null);
    }

    @Override
    public boolean isTCP() {
        return true;
    }

    @Override
    public int getDefaultPort() {
        return 5060;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final Object o) {
        channel().writeAndFlush(o);
        // System.err.println("Cmon, writing the stupid message!");
        // channel().write(o);
    }

    @Override
    public boolean connect() {
        return true;
    }

}
