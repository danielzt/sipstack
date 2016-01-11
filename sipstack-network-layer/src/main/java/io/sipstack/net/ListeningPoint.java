/**
 * 
 */
package io.sipstack.net;

import io.pkts.packet.sip.Transport;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.net.netty.NettyNetworkInterface;
import io.sipstack.netty.codec.sip.Connection;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static io.pkts.packet.sip.impl.PreConditions.assertNotNull;

/**
 * Simple wrapper around the actual listening address and the optional
 * vip address.
 * 
 * @author jonas@jonasborjesson.com
 */
public interface ListeningPoint {

    int getLocalPort();

    InetSocketAddress getLocalAddress();

    String getLocalIp();

    Transport getTransport();

    SipURI getListenAddress();

    Optional<SipURI> getVipAddress();

    /**
     * Bring this {@link ListeningPoint} up, as in have it start
     * listening on its desired ip and port.
     *
     * @return a future, which when successfully completed
     * indicates that we manage to listen to the given address
     */
    CompletableFuture<Void> up();

    /**
     * Bring this {@link ListeningPoint} down, as in have it stop
     * listening on its desired ip and port.
     *
     * @return a future, which when successfully completed
     * indicates that we manage to shut down the port that we
     * previously were listening on.
     */
    CompletableFuture<Void> down();

    /**
     * Connect to the remote address.
     *
     * @param remoteAddress
     * @return
     */
    CompletableFuture<Connection> connect(final InetSocketAddress remoteAddress);

    // because you can't override toString
    default String toStringRepresentation() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getListenAddress().toString());

        if (getVipAddress().isPresent()) {
            sb.append(" as ").append(getVipAddress().toString());
        }
        return sb.toString();
    }

}
