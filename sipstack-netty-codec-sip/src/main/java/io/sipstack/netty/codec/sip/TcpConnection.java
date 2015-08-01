/**
 * 
 */
package io.sipstack.netty.codec.sip;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;

/**
 *
 * @author jonas@jonasborjesson.com
 */
public final class TcpConnection extends AbstractConnection {


    public TcpConnection(final Channel channel, final InetSocketAddress remote) {
        super(Transport.tcp, channel, remote);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final Object o) {
        // channel().writeAndFlush(toByteBuf(msg));
        channel().writeAndFlush(o);
    }

    @Override
    public boolean connect() {
        return true;
    }

}
