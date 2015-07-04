/**
 * 
 */
package io.sipstack.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.sipstack.config.NetworkInterfaceConfiguration;
import io.sipstack.netty.codec.sip.InboundOutboundHandlerAdapter;
import io.sipstack.netty.codec.sip.SipMessageDatagramDecoder;
import io.sipstack.netty.codec.sip.SipMessageEncoder;
import io.sipstack.netty.codec.sip.SipMessageStreamDecoder;
import io.sipstack.netty.codec.sip.event.SipMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;

/**
 * The {@link NetworkLayer} is the glue between the network (netty) and
 * the rest of the SIP stack and eventually the users own application.
 * 
 * The purpose of this {@link NetworkLayer} is simply to read/write from/to
 * the channels and then dispatch the result of those operations to
 * the actual SIP Stack.
 * 
 * @author jonas@jonasborjesson.com
 */
public class NetworkLayer {

    private static final Logger logger = LoggerFactory.getLogger(NetworkLayer.class);

    /**
     * Every {@link ListeningPoint} has a reference to the very same {@link CountDownLatch}
     * and each of those {@link ListeningPoint}s will call {@link CountDownLatch#countDown()}
     * when they are finished shutting down the socket again. Hence, we can use this
     * to hang on the latch until everything is shut down again.
     */
    private final CountDownLatch latch;

    private final List<NetworkInterface> interfaces;


    /**
     * 
     */
    private NetworkLayer(final CountDownLatch latch, final List<NetworkInterface> ifs) {
        this.latch = latch;
        this.interfaces = ifs;
    }

    public void start() {
        this.interfaces.forEach(NetworkInterface::up);
    }

    public void sync() throws InterruptedException {
        this.latch.await();
    }

    public static Builder with(final List<NetworkInterfaceConfiguration> ifs) throws IllegalArgumentException {
        return new Builder(ensureNotNull(ifs));
    }

    public static class Builder {

        private final List<NetworkInterfaceConfiguration> ifs;

        private List<InboundOutboundHandlerAdapter> sipChannels = new ArrayList<>();

        private EventLoopGroup bossGroup;
        private EventLoopGroup workerGroup;
        private EventLoopGroup udpGroup;

        /**
         * The TCP based bootstrap.
         */
        private ServerBootstrap serverBootstrap;

        /**
         * Our UDP based bootstrap.
         */
        private Bootstrap bootstrap;

        private final Channel udpListeningPoint = null;

        private SimpleChannelInboundHandler<SipMessageEvent> serverHandler;

        private Optional<InboundOutboundHandlerAdapter> transportLayer = Optional.empty();
        private Optional<InboundOutboundHandlerAdapter> transactionLayer = Optional.empty();
        private Optional<InboundOutboundHandlerAdapter> transactionUser = Optional.empty();
        private Optional<InboundOutboundHandlerAdapter> application = Optional.empty();

        private Builder(final List<NetworkInterfaceConfiguration> ifs) {
            this.ifs = ifs;
        }

        public Builder withTransportLayer(final InboundOutboundHandlerAdapter handler) {
            transportLayer = Optional.ofNullable(handler);
            return this;
        }

        public Builder withBossEventLoopGroup(final EventLoopGroup group) {
            this.bossGroup = group;
            return this;
        }

        public Builder withTCPEventLoopGroup(final EventLoopGroup group) {
            this.workerGroup = group;
            return this;
        }

        public Builder withUDPEventLoopGroup(final EventLoopGroup group) {
            this.udpGroup = group;
            return this;
        }

        public Builder withTransactionLayer(final InboundOutboundHandlerAdapter handler) {
            transactionLayer = Optional.ofNullable(handler);
            return this;
        }

        public Builder withTransactionUser(final InboundOutboundHandlerAdapter handler) {
            transactionUser = Optional.ofNullable(handler);
            return this;
        }

        public Builder withApplication(final InboundOutboundHandlerAdapter handler) {
            application = Optional.ofNullable(handler);
            return this;
        }

        /*
        public Builder serverHandler(final SimpleChannelInboundHandler<SipMessageEvent> handler) {
            this.serverHandler = handler;
            return this;
        }
        */

        public NetworkLayer build() {

            // Need to create final a handler for final bridging between the final low level and final the application level
            // final. Cant have the final Server implement the final netty interfaces since final the Builder is final setting it final up.
            // Really should create final a NetworkActor etc final to handle final all of this.

            // TODO: check that if you e.g. specify dialog layer then you must also specify transaction layer

            if (bossGroup == null) {
                bossGroup = new NioEventLoopGroup();
            }

            if (workerGroup == null && udpGroup == null) {
                workerGroup = new NioEventLoopGroup();
                udpGroup = workerGroup;
            } else if (workerGroup != null && udpGroup == null) {
                udpGroup = workerGroup;
            } else if (udpGroup != null && workerGroup != null) {
                workerGroup = udpGroup;
            }

            final List<NetworkInterface.Builder> builders = new ArrayList<NetworkInterface.Builder>();
            if (this.ifs.isEmpty()) {
                final Inet4Address address = findPrimaryAddress();
                final String ip = address.getHostAddress();
                throw new IllegalArgumentException("Not finished so you have to specify at least one listening point");
            } else {
                this.ifs.forEach(i -> {
                    final NetworkInterface.Builder ifBuilder = NetworkInterface.with(i);
                    ifBuilder.udpBootstrap(ensureUDPBootstrap(this.serverHandler));
                    ifBuilder.tcpBootstrap(ensureTCPBootstrap(this.serverHandler));
                    builders.add(ifBuilder);
                });
            }

            final CountDownLatch latch = new CountDownLatch(builders.size());
            final List<NetworkInterface> ifs = new ArrayList<>();
            builders.forEach(ifBuilder -> ifs.add(ifBuilder.latch(latch).build()));
            return new NetworkLayer(latch, Collections.unmodifiableList(ifs));
        }

        private Bootstrap ensureUDPBootstrap(final SimpleChannelInboundHandler<SipMessageEvent> handler) {
            if (this.bootstrap == null) {
                final Bootstrap b = new Bootstrap();
                b.group(this.udpGroup)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(final DatagramChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new SipMessageDatagramDecoder());
                        pipeline.addLast("encoder", new SipMessageEncoder());
                        transportLayer.ifPresent(handler -> pipeline.addLast("transport", handler));
                        transactionLayer.ifPresent(handler -> pipeline.addLast("transaction", handler));
                        transactionUser.ifPresent(handler -> pipeline.addLast("tu", handler));
                        application.ifPresent(handler -> pipeline.addLast("application", handler));
                    }
                });

                this.bootstrap = b;
            }
            return this.bootstrap;
        }

        private ServerBootstrap ensureTCPBootstrap(final SimpleChannelInboundHandler<SipMessageEvent> handler) {
            if (this.serverBootstrap == null) {
                final ServerBootstrap b = new ServerBootstrap();

                b.group(this.bossGroup, this.workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(final SocketChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new SipMessageStreamDecoder());
                        pipeline.addLast("encoder", new SipMessageEncoder());
                        transportLayer.ifPresent(handler -> pipeline.addLast("transport", handler));
                        transactionLayer.ifPresent(handler -> pipeline.addLast("transaction", handler));
                        transactionUser.ifPresent(handler -> pipeline.addLast("tu", handler));
                        application.ifPresent(handler -> pipeline.addLast("application", handler));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);
                this.serverBootstrap = b;
            }
            return this.serverBootstrap;
        }

        private Inet4Address findPrimaryAddress() {
            java.net.NetworkInterface loopback = null; 
            java.net.NetworkInterface primary = null;
            try {
                final Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    final java.net.NetworkInterface i = interfaces.nextElement();
                    if (i.isLoopback()) {
                        loopback = i;
                    } else if (i.isUp() && !i.isVirtual()) {
                        primary = i;
                        break;
                    }
                }
            } catch (final SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            final java.net.NetworkInterface network = primary != null ? primary : loopback;
            final Inet4Address address = getInet4Address(network);
            return address;
        }

        private Inet4Address getInet4Address(final java.net.NetworkInterface network) {
            final Enumeration<InetAddress> addresses = network.getInetAddresses();
            while (addresses.hasMoreElements()) {
                final InetAddress address = addresses.nextElement();
                if (address instanceof Inet4Address) {
                    return (Inet4Address) address;
                }
            }
            return null;
        }

    }

}
