package io.sipstack.net.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.CompleteFuture;
import io.pkts.packet.sip.Transport;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.config.NetworkInterfaceConfiguration;
import io.sipstack.net.IllegalTransportException;
import io.sipstack.net.ListeningPoint;
import io.sipstack.net.NetworkInterface;
import io.sipstack.netty.codec.sip.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static io.pkts.packet.sip.impl.PreConditions.assertNotNull;
import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;

/**
 *
 * @author jonas@jonasborjesson.com
 */
public final class NettyNetworkInterface implements NetworkInterface, ChannelFutureListener {

    private final Logger logger = LoggerFactory.getLogger(NettyNetworkInterface.class);

    private final String name;

    private CountDownLatch latch;
    private final Bootstrap udpBootstrap;

    private final Bootstrap tcpBootstrap;

    private final ServerBootstrap tcpServerBootstrap;

    private final List<ListeningPoint> listeningPoints;

    private final ListeningPoint[] listeningPointsByTransport = new ListeningPoint[Transport.values().length];


    private NettyNetworkInterface(final String name, final Bootstrap udpBootstrap,
                                  final Bootstrap tcpBootstrap,
                                  final ServerBootstrap tcpServerBootstrap,
                                  final List<ListeningPoint> lps) {
        this.name = name;
        this.udpBootstrap = udpBootstrap;
        this.tcpBootstrap = tcpBootstrap;
        this.tcpServerBootstrap = tcpServerBootstrap;
        this.listeningPoints = lps;
        lps.forEach(lp -> listeningPointsByTransport[lp.getTransport().ordinal()] = lp);
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Bring this interface up, as in start listening to its dedicated listening points.
     */
    @Override
    public CompletableFuture<Void> up() {
        final List<CompletableFuture<Void>> futures = new CopyOnWriteArrayList<>();
        this.listeningPoints.forEach(lp -> {
            futures.add(lp.up());
        });
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public CompletableFuture<Void> down() {
        final List<CompletableFuture<Void>> futures = new CopyOnWriteArrayList<>();
        this.listeningPoints.forEach(lp -> {
            futures.add(lp.down());
        });
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private static Transport getTransport(final SipURI uri) {
        return Transport.valueOf(uri.getTransportParam().toString());
    }

    public static int getPort(final int port, final Transport transport) {
        if (port >= 0) {
            return port;
        }

        if (transport == Transport.tls) {
            return 5061;
        }

        if (transport == Transport.ws) {
            return 5062;
        }

        if (transport == Transport.sctp) {
            // TODO: not sure about this one but since
            // we currently do not support it then
            // let's leave it like this for now.
            return 5060;
        }

        return 5060;
    }

    @Override
    public ListeningPoint getListeningPoint(final Transport transport) {
        return listeningPointsByTransport[transport.ordinal()];
    }

    /**
     * Use this {@link NettyNetworkInterface} to connect to a remote address using the supplied
     * {@link Transport}.
     * 
     * Note, if the {@link Transport} is a connection less transport, such as UDP, then there isn't
     * a "connect" per se.
     * 
     * @param remoteAddress
     * @param transport
     * @return a {@link ChannelFuture} that, once completed, will contain the {@link Channel} that
     *         is connected to the remote address.
     * @throws IllegalTransportException in case the {@link NettyNetworkInterface} isn't configured with
     *         the specified {@link Transport}
     */
    @Override
    public CompletableFuture<Connection> connect(final Transport transport, final InetSocketAddress remoteAddress)
            throws IllegalTransportException {
        final ListeningPoint lp = listeningPointsByTransport[transport.ordinal()];
        if (lp == null) {
            final String msg = String.format("Interface \"%s\" is not listening on transport %s", name, transport);
            throw new IllegalTransportException(msg);
        }

        return lp.connect(remoteAddress);


        // if (transport == Transport.udp || transport == null) {
            // final ListeningPoint lp2 = listeningPointsByTransport[Transport.udp.ordinal()];
            // return this.udpBootstrap.connect(remoteAddress, lp.getLocalAddress());
            // return null;

            // final UdpConnection connection = new UdpConnection(lp.getChannel(), remoteAddress);
            // final ChannelFuture future = lp.getChannel().newSucceededFuture();
            // return future;

            /*
            f.addListener(new GenericFutureListener<ChannelFuture>(){

                @Override
                public void operationComplete(final ChannelFuture future) throws Exception {
                    System.err.println("Future success " + future.isSuccess());
                    final Channel channel = future.channel();
                    System.err.println("Future completed so I guess I'm connected " + channel);
                    System.err.println("Remote Address: " + channel.remoteAddress());
                    System.err.println("Local Address: " + channel.localAddress());

                }
            });
            */

            /*
            final InetSocketAddress remote2 = new InetSocketAddress("192.168.0.100", 8576);
            final ChannelFuture f2 = lp.getChannel().connect(remote2);
            f2.addListener(new GenericFutureListener<ChannelFuture>(){

                @Override
                public void operationComplete(final ChannelFuture future) throws Exception {
                    System.err.println("Future2 success " + future.isSuccess());
                    System.err.println("Future2 cause " + future.cause());
                    final Channel channel = future.channel();
                    System.err.println("Future2 completed so I guess I'm connected " + channel);
                    System.err.println("Remote2 Address: " + channel.remoteAddress());
                    System.err.println("Local2 Address: " + channel.localAddress());

                }
            });
            */
            // final UdpConnection connection = new UdpConnection(lp.getChannel(), remoteAddress);
            // return this.udpBootstrap.group().next().newSucceededFuture(connection);
        // }

        // TODO: TCP
        // TODO: TLS
        // TODO: WS
        // TODO: WSS

        // throw new IllegalTransportException("Stack has not been configured for transport " + transport);
    }


    static Builder with(final NetworkInterfaceConfiguration config) {
        assertNotNull(config);
        return new Builder(config);
    }


    public static class Builder {
        private final NetworkInterfaceConfiguration config;

        /**
         * Our netty boostrap for connection less protocols
         */
        private Bootstrap udpBootstrap;

        private Bootstrap tcpBootstrap;

        private ServerBootstrap tcpServerBootstrap;

        private CountDownLatch latch;


        private Builder(final NetworkInterfaceConfiguration config) {
            this.config = config;
        }

        public Builder latch(final CountDownLatch latch) {
            this.latch = latch;
            return this;
        }

        public Builder udpBootstrap(final Bootstrap bootstrap) {
            this.udpBootstrap = bootstrap;
            return this;
        }

        public Builder tcpBootstrap(final Bootstrap bootstrap) {
            this.tcpBootstrap = bootstrap;
            return this;
        }

        public Builder tcpServerBootstrap(final ServerBootstrap bootstrap) {
            this.tcpServerBootstrap = bootstrap;
            return this;
        }

        public NettyNetworkInterface build() {
            ensureNotNull(this.latch, "Missing the latch");
            if (this.config.hasUDP()) {
                ensureNotNull(this.udpBootstrap, "You must configure a connectionless bootstrap");
            }

            if (this.config.hasTCP()) {
                ensureNotNull(this.tcpBootstrap, "You must configure a connection oriented bootstrap");
            }

            if (this.config.hasTLS() || this.config.hasWS() || this.config.hasSCTP()) {
                throw new IllegalTransportException("Sorry, can only do TCP and UDP for now");
            }

            final SipURI listenAddress = this.config.getListeningAddress();
            final SipURI vipAddress = this.config.getVipAddress();
            final List<ListeningPoint> lps = new ArrayList<>();
            this.config.getTransports().forEach(t -> {
                final SipURI listen = SipURI.withTemplate(listenAddress).withTransport(t).build();
                final NettyListeningPoint lp = NettyListeningPoint.withListenAddress(listen)
                        .withTransport(t)
                        .withVipAddress(vipAddress)
                        .withTcpBootstrap(tcpBootstrap)
                        .withTcpServerBootstrap(tcpServerBootstrap)
                        .withUdpBootstrap(udpBootstrap)
                        .build();
                lps.add(lp);
            });

            return new NettyNetworkInterface(this.config.getName(),
                    this.udpBootstrap,
                    this.tcpBootstrap,
                    this.tcpServerBootstrap,
                    Collections.unmodifiableList(lps));
        }

    }


    @Override
    public void operationComplete(final ChannelFuture future) throws Exception {
        // TODO Auto-generated method stub

    }

}
