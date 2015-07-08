/**
 * 
 */
package io.sipstack.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.netty.codec.sip.Transport;

import java.net.SocketAddress;
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
    private final SipURI vipAddress;
    private final Transport transport;

    private final AtomicReference<Channel> channel = new AtomicReference<>();

    /**
     * 
     */
    private ListeningPoint(final Transport transport, final SipURI listenAddress, final SipURI vipAddress) {
        this.listenAddress = listenAddress;
        this.vipAddress = vipAddress;
        this.transport = transport;
    }

    public static ListeningPoint create(final Transport transport, final SipURI listen, final SipURI vipAddress) {
        assertNotNull(transport);
        assertNotNull(listen);
        assertNotNull(vipAddress);
        return new ListeningPoint(transport, listen, vipAddress);
    }

    public Transport getTransport() {
        return this.transport;
    }

    public SipURI getListenAddress() {
        return this.listenAddress;
    }

    public SipURI getVipAddress() {
        return this.vipAddress;
    }

    // not nice but...
    public void setChannel(final Channel channel) {
        this.channel.set(channel);
    }

    public Channel getChannel() {
        return this.channel.get();
    }

    public ChannelFuture connect(final SocketAddress address) {
        final Channel channel = this.channel.get();
        return channel.connect(address, channel.voidPromise());
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
