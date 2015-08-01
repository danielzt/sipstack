package io.sipstack.net;

import io.netty.channel.ChannelFuture;
import io.sipstack.netty.codec.sip.Transport;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public interface NetworkLayer {

    /**
     * Start the {@link NetworkLayer}, which will call {@link NetworkInterface#up()} on all
     * the configured network interfaces.
     */
    void start();

    /**
     * Attempt to connect to the specified address using the specified transport protocol.
     * The default {@link NetworkInterface} will be used.
     *
     * @param address
     * @param transport
     * @return
     */
    ChannelFuture connect(InetSocketAddress address, Transport transport);

    /**
     * Hang on this network layer until all interfaces have been shutdown and as such
     * this network is stopped.
     *
     * @throws InterruptedException
     */
    void sync() throws InterruptedException;

    /**
     * Same as {@link NetworkLayer#getListeningPoint(String, Transport)} but we will
     * grab the default {@link NetworkInterface}.
     *
     * @param ip
     * @param port
     * @param transport
     * @return
     */
    Optional<ListeningPoint> getListeningPoint(Transport transport);

    /**
     * Try and get a listening point that can be used to send messages across using the
     * specified {@link Transport} over the named {@link NetworkInterface}.
     *
     * @param networkInterfaceName
     * @param transport
     * @return
     */
    Optional<ListeningPoint> getListeningPoint(String networkInterfaceName, Transport transport);
}
