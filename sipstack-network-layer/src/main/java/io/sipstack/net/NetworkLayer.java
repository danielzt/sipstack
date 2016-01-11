package io.sipstack.net;

import io.netty.channel.ChannelFuture;
import io.pkts.packet.sip.Transport;
import io.sipstack.netty.codec.sip.Connection;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author jonas@jonasborjesson.com
 */
public interface NetworkLayer {

    /**
     * Start the {@link NetworkLayer}, which will call {@link NetworkInterface#up()} on all
     * the configured network interfaces. The method will hang until all network interfaces
     * have completed, either successfully or in error.
     */
    void start();

    /**
     * Attempt to connect to the specified address using the specified transport protocol.
     * The default {@link NetworkInterface} will be used.
     *
     * This is a convenience method for
     *
     * @param address
     * @param transport
     * @return
     */
    CompletableFuture<Connection> connect(Transport transport, InetSocketAddress address);

    /**
     * Hang on this network layer until all interfaces have been shutdown and as such
     * this network is stopped.
     *
     * @throws InterruptedException
     */
    void sync() throws InterruptedException;

    /**
     * A {@link NetworkLayer} will always have a default {@link NetworkInterface}, which
     * has either been explicitly configured or else it will simply be the first one
     * configured with this {@link NetworkLayer}.
     *
     * @return
     */
    NetworkInterface getDefaultNetworkInterface();

    /**
     * Get the named {@link NetworkInterface}.
     *
     * @param name the name of the interface
     * @return an optional with the named interface if found,
     * otherwise an empty optional will be returned.
     */
    Optional<? extends NetworkInterface> getNetworkInterface(String name);

    /**
     * Get a list of all the {@link NetworkInterface}s that has been configured.
     *
     * @return
     */
    List<? extends NetworkInterface> getNetworkInterfaces();

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
