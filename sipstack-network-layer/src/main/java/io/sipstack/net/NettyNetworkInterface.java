package io.sipstack.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.config.NetworkInterfaceConfiguration;
import io.sipstack.netty.codec.sip.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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

    private final ServerBootstrap tcpBootstrap;

    private final List<ListeningPoint> listeningPoints;

    private final ListeningPoint[] listeningPointsByTransport = new ListeningPoint[Transport.values().length];


    private NettyNetworkInterface(final String name, final Bootstrap udpBootstrap, final ServerBootstrap tcpBootstrap,
                                  final List<ListeningPoint> lps) {
        this.name = name;
        this.udpBootstrap = udpBootstrap;
        this.tcpBootstrap = tcpBootstrap;
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
    public void up() {

        final CountDownLatch bindLatch = new CountDownLatch(this.listeningPoints.size());
        final List<ListeningPoint> errors = Collections.synchronizedList(new ArrayList<ListeningPoint>());
        this.listeningPoints.forEach(lp -> {
            final ListeningPoint listeningPoint = lp;
            final Transport transport = lp.getTransport();

            ChannelFuture future = null;
            if (transport == Transport.udp) {
                future = this.udpBootstrap.bind(lp.getLocalAddress());
            } else if (transport == Transport.tcp) {
                future = this.tcpBootstrap.bind(lp.getLocalAddress());
            } else {
                // should already have been ensured elsewhere but let's check again
                throw new IllegalTransportException("Can only do UDP and TCP for now");
            }

            future.addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(final ChannelFuture future) throws Exception {
                    if (future.isDone() && future.isSuccess()) {
                        final Channel channel = future.channel();
                        listeningPoint.setChannel(channel);
                        NettyNetworkInterface.this.logger.info("Successfully bound to listening point: " + listeningPoint);
                    } else if (future.isDone() && !future.isSuccess()) {
                        NettyNetworkInterface.this.logger.info("Unable to bind to listening point: " + listeningPoint);
                        errors.add(listeningPoint);
                    }

                    /*
                    if (listeningPoint.getTransport() == Transport.udp) {
                        final InetSocketAddress local2 = new InetSocketAddress("127.0.0.1", 5060);
                        final InetSocketAddress remote2 = new InetSocketAddress("10.0.1.30", 5080);
                        final ChannelFuture con2 = NettyNetworkInterface.this.udpBootstrap.connect(remote2, local2);
                        con2.addListener(new GenericFutureListener<ChannelFuture>() {

                            @Override
                            public void operationComplete(final ChannelFuture future) throws Exception {
                                System.err.println("Connection2 success " + future.isSuccess());
                                System.err.println("Connection2 cause " + future.cause());
                                final Channel channel = future.channel();
                                System.err.println("Connection2 completed so I guess I'm connected " + channel);
                                System.err.println("Connection2 Address: " + channel.remoteAddress());
                                System.err.println("Connection2 Address: " + channel.localAddress());
                            }
                        });
                    }
                    */

                    bindLatch.countDown();
                }
            });
        });

        try {
            bindLatch.await();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("Unable to bind to one or more listening points");
        }

    }

    @Override
    public void down() {

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
    public ChannelFuture connect(final InetSocketAddress remoteAddress, final Transport transport)
            throws IllegalTransportException {
        if (transport == Transport.udp || transport == null) {
            final ListeningPoint lp = listeningPointsByTransport[Transport.udp.ordinal()];
            return this.udpBootstrap.connect(remoteAddress, lp.getLocalAddress());

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
        }

        // TODO: TCP
        // TODO: TLS
        // TODO: WS
        // TODO: WSS

        throw new IllegalTransportException("Stack has not been configured for transport " + transport);
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

        private ServerBootstrap tcpBootstrap;

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

        public Builder tcpBootstrap(final ServerBootstrap bootstrap) {
            this.tcpBootstrap = bootstrap;
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
                final SipURI listen = SipURI.withTemplate(listenAddress).withTransport(t.toString()).build();
                final SipURI vipClone = vipAddress != null ? vipAddress.clone() : null;
                lps.add(ListeningPoint.create(t, listen, vipClone));
            });

            return new NettyNetworkInterface(this.config.getName(), this.udpBootstrap, this.tcpBootstrap,
                    Collections.unmodifiableList(lps));
        }

    }


    @Override
    public void operationComplete(final ChannelFuture future) throws Exception {
        // TODO Auto-generated method stub

    }

}
