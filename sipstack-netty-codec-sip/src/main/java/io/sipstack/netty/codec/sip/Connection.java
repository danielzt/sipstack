/**
 * 
 */
package io.sipstack.netty.codec.sip;

import io.netty.channel.Channel;
import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.Transport;
import io.pkts.packet.sip.address.SipURI;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Represents a connection between two end-points and its primary purpose is to
 * encapsulate specific knowledge of which type of underlying {@link Channel} is
 * being used.
 * 
 * @author jonas@jonasborjesson.com
 */
public interface Connection {

    ConnectionId id();

    void storeObject(Object o);

    Optional<Object> fetchObject();

    /**
     * A connection may optionally have a VIP address, which for the
     * actual connection itself doesn't matter but there are cases
     * where you e.g. want to stamp a different address, a VIP address,
     * in the Via and Contact-headers of your SIP message. This is
     * common when you have some sort of load balancer or your machine
     * is NAT:ed and therefore, you want to have that external facing
     * address stamped instead.
     *
     * @return
     */
    Optional<SipURI> getVipAddress();

    /**
     * Get the local port to which this {@link Connection} is listening to.
     * 
     * @return
     */
    int getLocalPort();

    /**
     * Just a convenience method for obtaining the default port for this
     * type of connection. If the connection represents a UDP "connection" or
     * a TCP connection then 5060 will be returned. If the connection is
     * TLS then 5061 will be returned.
     *
     * @return
     */
    int getDefaultPort();

    /**
     * Get the local ip-address to which this {@link Connection} is listening to
     * as a byte-array.
     * 
     * @return
     */
    byte[] getRawLocalIpAddress();

    /**
     * Get the local ip-address to which this {@link Connection} is listening to
     * as a {@link String}.
     * 
     * @return
     */
    String getLocalIpAddress();

    InetSocketAddress getLocalAddress();

    Buffer getLocalIpAddressAsBuffer();

    /**
     * Get the remote address to which this {@link Connection} is connected to.
     * 
     * @return
     */
    InetSocketAddress getRemoteAddress();

    /**
     * Get the remote port to which this {@link Connection} is connected to.
     * 
     * @return
     */
    int getRemotePort();

    /**
     * Get the remote ip-address to which this {@link Connection} is connected
     * to as a byte-array.
     * 
     * @return
     */
    byte[] getRawRemoteIpAddress();

    /**
     * Get the remote ip-address to which this {@link Connection} is connected
     * to as a {@link String}.
     * 
     * @return
     */
    String getRemoteIpAddress();

    Buffer getRemoteIpAddressAsBuffer();

    Transport getTransport();

    /**
     * Check whether or not this {@link Connection} is using UDP as its
     * underlying transport protocol.
     * 
     * @return
     */
    boolean isUDP();

    /**
     * Check whether or not this {@link Connection} is using TCP as its
     * underlying transport protocol.
     * 
     * @return
     */
    boolean isTCP();

    /**
     * Check whether or not this {@link Connection} is using TLS as its
     * underlying transport protocol.
     * 
     * @return
     */
    boolean isTLS();

    /**
     * Check whether or not this {@link Connection} is using SCTP as its
     * underlying transport protocol.
     * 
     * @return
     */
    boolean isSCTP();

    /**
     * Check whether or not this {@link Connection} is using websocket as its
     * underlying transport protocol.
     * 
     * @return
     */
    boolean isWS();

    /**
     * Send an Object over this connection.
     * 
     * @param msg
     */
    void send(Object o);

    boolean connect();

    void close();

}
