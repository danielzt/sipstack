package io.sipstack.netty.codec.sip;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;

/**
 * Encapsulates a
 * 
 * @author jonas@jonasborjesson.com
 */
public final class UdpConnection extends AbstractConnection {

    public UdpConnection(final Channel channel, final InetSocketAddress remoteAddress) {
        super(Transport.udp, channel, remoteAddress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUDP() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final Object o) {
        // final DatagramPacket pkt = new DatagramPacket(toByteBuf(msg), getRemoteAddress());
        // channel().writeAndFlush(pkt);
        // System.err.println("UDPConnection: sending");

        // final SipMessageIOEventImpl event = new SipMessageIOEventImpl(this, msg, System.currentTimeMillis());
        // channel().writeAndFlush(event);
        channel().writeAndFlush(o);
    }

    @Override
    public boolean connect() {
        return true;
    }



}
