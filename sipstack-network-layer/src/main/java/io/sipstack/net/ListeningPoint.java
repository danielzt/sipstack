/**
 * 
 */
package io.sipstack.net;

import io.netty.channel.Channel;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.netty.codec.sip.Transport;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static io.pkts.packet.sip.impl.PreConditions.assertNotNull;

/**
 * Simple wrapper around the actual listening address and the optional
 * vip address.
 * 
 * @author jonas@jonasborjesson.com
 */
public final class ListeningPoint {


    private final SipURI listenAddress;
    private final Optional<SipURI> vipAddress;
    private final Transport transport;
    private final InetSocketAddress localAddress;
    private final int localPort;

    private final AtomicReference<Channel> channel = new AtomicReference<>();

    /**
     * 
     */
    private ListeningPoint(final Transport transport, final SipURI listenAddress, final SipURI vipAddress) {
        this.listenAddress = listenAddress;
        this.vipAddress = Optional.ofNullable(vipAddress);
        this.transport = transport;
        this.localPort = NettyNetworkInterface.getPort(listenAddress.getPort(), transport);
        this.localAddress = new InetSocketAddress(listenAddress.getHost().toString(), this.localPort);
    }

    public static ListeningPoint create(final Transport transport, final SipURI listen, final SipURI vipAddress) {
        assertNotNull(transport);
        assertNotNull(listen);
        return new ListeningPoint(transport, listen, vipAddress);
    }

    public int getLocalPort() {
        return localPort;
    }

    public SocketAddress getLocalAddress() {
        return this.localAddress;
    }

    public Transport getTransport() {
        return this.transport;
    }

    public SipURI getListenAddress() {
        return this.listenAddress;
    }

    public Optional<SipURI> getVipAddress() {
        return this.vipAddress;
    }

    // not nice but...
    public void setChannel(final Channel channel) {
        this.channel.set(channel);
    }

    public Channel getChannel() {
        return this.channel.get();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.listenAddress.toString());

        if (this.vipAddress != null) {
            sb.append(" as ").append(this.vipAddress.toString());
        }
        return sb.toString();
    }

}
