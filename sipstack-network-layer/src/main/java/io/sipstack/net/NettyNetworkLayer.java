/**
 * 
 */
package io.sipstack.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.sipstack.config.NetworkInterfaceConfiguration;
import io.sipstack.netty.codec.sip.SipMessageDatagramDecoder;
import io.sipstack.netty.codec.sip.SipMessageEncoder;
import io.sipstack.netty.codec.sip.SipMessageStreamDecoder;
import io.sipstack.netty.codec.sip.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;

/**
 * The {@link NettyNetworkLayer} is the glue between the network (netty) and
 * the rest of the SIP stack and eventually the users own io.sipstack.application.application.
 * 
 * The purpose of this {@link NettyNetworkLayer} is simply to read/write from/to
 * the channels and then dispatch the result of those operations to
 * the actual SIP Stack.
 * 
 * @author jonas@jonasborjesson.com
 */
public class NettyNetworkLayer implements NetworkLayer {

    private static final Logger logger = LoggerFactory.getLogger(NettyNetworkLayer.class);

    /**
     * Every {@link ListeningPoint} has a reference to the very same {@link CountDownLatch}
     * and each of those {@link ListeningPoint}s will call {@link CountDownLatch#countDown()}
     * when they are finished shutting down the socket again. Hence, we can use this
     * to hang on the latch until everything is shut down again.
     */
    private final CountDownLatch latch;

    private final List<NettyNetworkInterface> interfaces;

    private final NettyNetworkInterface defaultInterface;

    /**
     * 
     */
    private NettyNetworkLayer(final CountDownLatch latch, final List<NettyNetworkInterface> ifs) {
        this.latch = latch;
        this.interfaces = ifs;

        // TODO: make this configurable. For now, it is simply the
        // first one...
        this.defaultInterface = ifs.get(0);
    }

    @Override
    public void start() {
        this.interfaces.forEach(NettyNetworkInterface::up);
    }

    @Override
    public ChannelFuture connect(final InetSocketAddress address, final Transport transport) {
        return this.defaultInterface.connect(address, transport);
    }

    @Override
    public void sync() throws InterruptedException {
        this.latch.await();
    }

    public static Builder with(final List<NetworkInterfaceConfiguration> ifs) throws IllegalArgumentException {
        return new Builder(ensureNotNull(ifs));
    }

    public static class Builder {

        private final List<NetworkInterfaceConfiguration> ifs;

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

        private Optional<InboundOutboundHandlerAdapter> handler = Optional.empty();

        private Builder(final List<NetworkInterfaceConfiguration> ifs) {
            this.ifs = ifs;
        }

        public Builder withHandler(final InboundOutboundHandlerAdapter handler) {
            this.handler = Optional.ofNullable(handler);
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

        public NettyNetworkLayer build() {

            // Need to create final a handler for final bridging between the final low level and final the io.sipstack.application.application level
            // final. Cant have the final Server implement the final netty interfaces since final the Builder is final setting it final up.
            // Really should create final a NetworkActor etc final to handle final all of this.

            // TODO: check that if you e.g. specify dialog layer then you must also specify io.sipstack.transaction.transaction layer

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

            final List<NettyNetworkInterface.Builder> builders = new ArrayList<NettyNetworkInterface.Builder>();
            if (this.ifs.isEmpty()) {
                final Inet4Address address = findPrimaryAddress();
                final String ip = address.getHostAddress();
                throw new IllegalArgumentException("Not finished so you have to specify at least one listening point");
            } else {
                this.ifs.forEach(i -> {
                    final NettyNetworkInterface.Builder ifBuilder = NettyNetworkInterface.with(i);
                    ifBuilder.udpBootstrap(ensureUDPBootstrap());
                    ifBuilder.tcpBootstrap(ensureTCPBootstrap());
                    builders.add(ifBuilder);
                });
            }

            final CountDownLatch latch = new CountDownLatch(builders.size());
            final List<NettyNetworkInterface> ifs = new ArrayList<>();
            builders.forEach(ifBuilder -> ifs.add(ifBuilder.latch(latch).build()));
            return new NettyNetworkLayer(latch, Collections.unmodifiableList(ifs));
        }

        private Bootstrap ensureUDPBootstrap() {
            if (this.bootstrap == null) {
                final Bootstrap b = new Bootstrap();
                b.channelFactory()
                b.group(this.udpGroup)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(final DatagramChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new SipMessageDatagramDecoder());
                        pipeline.addLast("encoder", new SipMessageEncoder());
                        handler.ifPresent(handler -> pipeline.addLast("handler", handler));
                    }
                }).option(ChannelOption.SO_REUSEADDR, true);

                this.bootstrap = b;
            }
            return this.bootstrap;
        }

        private ServerBootstrap ensureTCPBootstrap() {
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
                        handler.ifPresent(handler -> pipeline.addLast("handler", handler));
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
