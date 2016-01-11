package io.sipstack.net.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.pkts.packet.sip.Transport;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.net.IllegalTransportException;
import io.sipstack.net.ListeningPoint;
import io.sipstack.netty.codec.sip.*;
import io.sipstack.netty.codec.sip.event.ConnectionActiveIOEvent;
import io.sipstack.netty.codec.sip.event.ConnectionIOEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static io.pkts.packet.sip.impl.PreConditions.assertNotNull;

/**
 * @author jonas@jonasborjesson.com
 */
public abstract class NettyListeningPoint implements ListeningPoint {

    protected static final Logger logger = LoggerFactory.getLogger(NettyListeningPoint.class);

    private final SipURI listenAddress;
    private final Optional<SipURI> vipAddress;
    private final Transport transport;
    private final InetSocketAddress localAddress;
    private final int localPort;

    protected final Clock clock;

    /**
     * @param transport
     * @param listenAddress
     * @param vipAddress
     */
    private NettyListeningPoint(final Transport transport,
                                final SipURI listenAddress,
                                final SipURI vipAddress,
                                final Clock clock) {
        this.listenAddress = listenAddress;
        this.vipAddress = Optional.ofNullable(vipAddress);
        this.transport = transport;
        this.localPort = NettyNetworkInterface.getPort(listenAddress.getPort(), transport);
        this.localAddress = new InetSocketAddress(listenAddress.getHost().toString(), this.localPort);
        this.clock = clock;
    }

    @Override
    public String toString() {
        return toStringRepresentation();
    }

    @Override
    public final int getLocalPort() {
        return localPort;
    }

    @Override
    public final InetSocketAddress getLocalAddress() {
        return this.localAddress;
    }

    @Override
    public final String getLocalIp() {
        return this.localAddress.getHostString();
    }

    @Override
    public final Transport getTransport() {
        return this.transport;
    }

    @Override
    public final SipURI getListenAddress() {
        return this.listenAddress;
    }

    @Override
    public final Optional<SipURI> getVipAddress() {
        return this.vipAddress;
    }

    static Builder withListenAddress(final SipURI address) {
        assertNotNull(address, "The listen address cannot be null");
        return new Builder(address);
    }

    /**
     * Listening point for UDP
     */
    private static class NettyUdpListeningPoint extends NettyListeningPoint {

        private final Bootstrap bootstrap;

        /**
         * For UDP we will use this channel for all {@link Connection}s we create
         * since we won't actually connect for real.
         */
        private AtomicReference<Channel> udpChannel = new AtomicReference<>();

        /**
         * @param transport
         * @param listenAddress
         * @param vipAddress
         */
        private NettyUdpListeningPoint(final SipURI listenAddress,
                                       final SipURI vipAddress,
                                       final Bootstrap bootstrap,
                                       final Clock clock) {
            super(Transport.udp, listenAddress, vipAddress, clock);
            this.bootstrap = bootstrap;
        }

        private void setChannel(final Channel channel) {
            this.udpChannel.set(channel);
        }

        @Override
        public CompletableFuture<Void> up() {
            final CompletableFuture<Void> future = new CompletableFuture<>();

            final ChannelFuture channelFuture = this.bootstrap.bind(getLocalAddress());
            channelFuture.addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        setChannel(channelFuture.channel());
                        NettyListeningPoint.logger.info("Successfully bound to listening point: " + getListenAddress());
                        future.complete(null);
                    } else {
                        NettyListeningPoint.logger.info("Unable to bind to listening point: " + getListenAddress());
                        future.completeExceptionally(channelFuture.cause());
                    }
                }
            });
            return future;
        }

        @Override
        public CompletableFuture<Void> down() {
            return null;
        }

        @Override
        public CompletableFuture<Connection> connect(final InetSocketAddress remoteAddress) {
            // Since we don't actually connect when using UDP we will be firing off
            // a user event stating that a new connection just became active.
            // Note: since we don't know when the connection goes away, this can
            // only be one part of the overall solution. For the full stack, this
            // is being handled by the transport layer...
            final Channel channel = udpChannel.get();
            final ChannelHandlerContext ctx = channel.pipeline().firstContext();
            final Connection connection = new UdpConnection(channel, remoteAddress, getVipAddress());
            final Long arrivalTime = clock.getCurrentTimeMillis();
            ctx.fireUserEventTriggered(ConnectionActiveIOEvent.create(connection, arrivalTime));
            return CompletableFuture.completedFuture(connection);
        }
    }

    /**
     * Listening point for TCP
     */
    private static class NettyTcpListeningPoint extends NettyListeningPoint {

        private final Bootstrap bootstrap;
        private final ServerBootstrap serverBootstrap;

        /**
         * @param transport
         * @param listenAddress
         * @param vipAddress
         */
        private NettyTcpListeningPoint(final SipURI listenAddress,
                                       final SipURI vipAddress,
                                       final Bootstrap bootstrap,
                                       final ServerBootstrap serverBootstrap,
                                       final Clock clock) {
            super(Transport.tcp, listenAddress, vipAddress, clock);
            this.bootstrap = bootstrap;
            this.serverBootstrap = serverBootstrap;
        }

        @Override
        public CompletableFuture<Void> up() {
            final CompletableFuture<Void> future = new CompletableFuture<>();

            final ChannelFuture channelFuture = this.serverBootstrap.bind(getLocalAddress());
            channelFuture.addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isDone() && channelFuture.isSuccess()) {
                        NettyListeningPoint.logger.info("Successfully bound to listening point: " + getListenAddress());
                        future.complete(null);
                    } else if (channelFuture.isDone() && !channelFuture.isSuccess()) {
                        NettyListeningPoint.logger.info("Unable to bind to listening point: " + getListenAddress());
                        future.completeExceptionally(channelFuture.cause());
                    }
                }
            });
            return future;
        }

        @Override
        public CompletableFuture<Void> down() {
            return null;
        }

        @Override
        public CompletableFuture<Connection> connect(final InetSocketAddress remoteAddress) {
            final CompletableFuture<Connection> f = new CompletableFuture<>();
            final ChannelFuture channelFuture = bootstrap.connect(remoteAddress);
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(final ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        final Channel channel = channelFuture.channel();
                        final Connection c = new TcpConnection(channel, remoteAddress);
                        f.complete(c);
                    } else {
                        f.completeExceptionally(future.cause());
                    }
                }
            });
            return f;
        }
    }

    static class Builder {
        private final SipURI listenAddress;
        private Transport transport;
        private SipURI vipAddress;

        private Bootstrap udpBootstrap;

        private Bootstrap tcpBootstrap;

        private ServerBootstrap tcpServerBootstrap;

        private Clock clock;

        private Builder(final SipURI address) {
            this.listenAddress = address;
        }

        public Builder withClock(final Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder withVipAddress(final SipURI vipAddress) {
            this.vipAddress = vipAddress;
            return this;
        }

        public Builder withTransport(final Transport transport) {
            this.transport = transport;
            return this;
        }

        public Builder withTransport(final SipURI vipAddress) {
            this.vipAddress = vipAddress;
            return this;
        }

        public Builder withUdpBootstrap(final Bootstrap bootstrap) {
            this.udpBootstrap = bootstrap;
            return this;
        }

        public Builder withTcpBootstrap(final Bootstrap bootstrap) {
            this.tcpBootstrap = bootstrap;
            return this;
        }

        public Builder withTcpServerBootstrap(final ServerBootstrap bootstrap) {
            this.tcpServerBootstrap = bootstrap;
            return this;
        }

        public NettyListeningPoint build() {
            assertNotNull(transport, "You must specify a transport");
            final Clock clock = this.clock != null ? this.clock : new SystemClock();
            if (transport.isTCP()) {
                assertNotNull(tcpBootstrap, "You must specify the TCP bootstrap");
                assertNotNull(tcpServerBootstrap, "You must specify the TCP bootstrap");
                return new NettyTcpListeningPoint(listenAddress, vipAddress, tcpBootstrap, tcpServerBootstrap, clock);
            } else if (transport.isUDP()) {
                assertNotNull(udpBootstrap, "You must specify the UDP bootstrap");
                return new NettyUdpListeningPoint(listenAddress, vipAddress, udpBootstrap, clock);
            }

            throw new IllegalTransportException("Currently we only support UDP and TCP");
        }

    }

}
