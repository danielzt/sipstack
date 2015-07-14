package io.sipstack.net;

import io.netty.channel.ChannelFuture;
import io.sipstack.netty.codec.sip.Transport;

import java.net.InetSocketAddress;

/**
 * @author jonas@jonasborjesson.com
 */
public interface NetworkInterface {

    /**
     * Get the friendly name of this interface.
     *
     * @return
     */
    String getName();

    /**
     * Bring this interface up, as in start listening to its dedicated listening points.
     */
    void up();

    void down();

    /**
     * Use this {@link NetworkInterface} to connect to a remote address using the supplied
     * {@link Transport}.
     *
     * Note, if the {@link Transport} is a connection less transport, such as UDP, then there isn't
     * a "connect" per se.
     *
     * @param remoteAddress
     * @param transport
     * @return a {@link ChannelFuture} that, once completed, will contain the {@link Channel} that
     *         is connected to the remote address.
     * @throws IllegalTransportException in case the {@link NetworkInterface} isn't configured with
     *         the specified {@link Transport}
     */
    ChannelFuture connect(final InetSocketAddress remoteAddress, final Transport transport);
}
