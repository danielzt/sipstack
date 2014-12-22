/**
 * 
 */
package io.sipstack.server;

import static io.pkts.packet.sip.impl.PreConditions.assertNotNull;
import io.netty.channel.Channel;
import io.pkts.packet.sip.address.SipURI;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple wrapper around the actual listen address and the optional
 * vip address.
 * 
 * @author jonas@jonasborjesson.com
 */
public final class ListeningPoint {


    private final SipURI listenAddress;
    private final SipURI vipAddress;

    private final AtomicReference<Channel> channel = new AtomicReference<>();

    /**
     * 
     */
    private ListeningPoint(final SipURI listenAddress, final SipURI vipAddress) {
        this.listenAddress = listenAddress;
        this.vipAddress = vipAddress;
    }

    public static ListeningPoint create(final SipURI listen, final SipURI vipAddress) {
        assertNotNull(listen);
        assertNotNull(vipAddress);
        return new ListeningPoint(listen, vipAddress);
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
