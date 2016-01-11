package io.sipstack.example.proxy.simple003;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.Transport;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.header.RouteHeader;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.netty.codec.sip.*;
import io.sipstack.netty.codec.sip.event.ConnectionIOEvent;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.netty.codec.sip.event.SipMessageIOEvent;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * This is a very simple proxy example written on top of the raw
 * netty + pkts.io support as exposed through sipstack.io.
 *
 * The main difference between this proxy example and the proxy 001
 * example is that we are now also listening to TCP and as such, we
 * actually have to pay attention to which protocol we are supposed
 * to use when we proxy to the next destination.
 *
 * @author jonas@jonasborjesson.com
 */
@Sharable
public class Proxy extends SimpleChannelInboundHandler<SipMessageIOEvent> {

    private final SimpleProxyConfiguration config;

    private Bootstrap tcpBootstrap;

    private Channel udpChannel;

    private final InetSocketAddress localAddress;

    public Proxy(final InetSocketAddress localAddress, final SimpleProxyConfiguration config) {
        this.localAddress = localAddress;
        this.config = config;
        System.out.println("Starting " + config.getName());
    }

    /**
     * This is ugly, and a catch 22, but we need to know the channel so
     * that we can create outbound connections in order to send
     *
     * messages there.
     *
     * @param udpChannel
     */
    public void setUdpChannel(final Channel udpChannel) {
        this.udpChannel = udpChannel;
    }

    public void setTcpStack(final Bootstrap stack) {
        this.tcpBootstrap = stack;
    }

    /**
     * We want to re-use the connection objects as much as possible so
     * therefore we will be storing the connections over which we
     * receive messages and the key will be the remote IP:port from
     * which the connection originated.
     */
    private final Map<ConnectionEndpointId, Connection> connections = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageIOEvent event) throws Exception {
        final Connection connection = event.connection();
        if (connection.isTCP()) {
            connections.put(connection.id().getRemoteConnectionEndpointId(), connection);
        }

        final SipMessage msg = event.message();

        if (msg.isRequest()) {
            final SipURI next = getNextHop(msg.toRequest());
            proxyTo(next, connection, msg.toRequest());
        } else {
            // Responses follow via headers to just pop the top-most via header
            // and then use the information found in the second via.
            // Of course, in a real stack we should check that the top-most via
            // actually pointed to us but this is a simple example so we'll continue
            // to live in happy land...
            final ConnectionId connectionId = ConnectionId.decode(msg.getViaHeader().getParameter("connection"));
            final SipResponse response = msg.toResponse().copy().withPoppedVia().build();
            final Consumer<Connection> onSuccess = c -> {
                final ConnectionEndpointId cid = c.id().getRemoteConnectionEndpointId();
                if (c.isTCP()) {
                    connections.put(cid, c);
                }
                c.send(IOEvent.create(c, response));
            };
            connect(connectionId, onSuccess);
        }
    }

    /**
     * The difference between Proxy example 001 and 002 is that we
     * now will also pay attention to the protocol for the "proxied leg".
     * Previously we just assumed UDP but we will now actually look
     * at the target and and check the protocol we should be using.
     *
     * @param destination
     * @param connection the connection over which we received the sip message.
     *                   We need this connection to stamp the correct information
     *                   in the Via-header that came in across
     * @param msg
     */
    private void proxyTo(final SipURI destination, final Connection connection, final SipRequest msg) {

        // Create a new Via-header that points back to us
        final Transport transport = destination.getTransportParam().orElse(Transport.udp);
        final ViaHeader via = ViaHeader.withHost(config.getListenAddress())
                .withPort(config.getListenPort())
                .withTransport(transport)
                .withBranch()
                .withRPortFlag()
                // We want to re-use the same connection as the message came in over so
                // we will be storing the connection id on the Via-header and will use
                // it to lookup the connection for any responses coming back.
                .withParameter(Buffers.wrap("connection"), connection.id().encode())
                .build();

        // Create the proxy request, where we will add our Via-header
        // as well as decrement the Max-Forwards header.
        final SipMessage proxyMsg = msg.copy()
                .withTopMostViaHeader(via)
                .onViaHeader((index, v) -> {
                    if (index == 1) {
                        // Add the received and rport information to the
                        // previous top-most Via, which is now our second
                        // Via on the list and since we start counting at
                        // zero, the index for the second via is 1...
                        v.withReceived(connection.getRemoteIpAddressAsBuffer()).withRPort(connection.getRemotePort());
                    }
                })
                .onMaxForwardsHeader(max -> max.decrement())
                .build();

        final String remoteHost = destination.getHost().toString();
        final int remotePort = destination.getPort();

        final InetSocketAddress remoteAddress = new InetSocketAddress(remoteHost, remotePort);
        final ConnectionId id = ConnectionId.create(transport, localAddress, remoteAddress);
        final Connection outboundConnection = connections.get(id.getRemoteConnectionEndpointId());

        if (outboundConnection != null) {
            outboundConnection.send(IOEvent.create(outboundConnection, proxyMsg));
        } else {
            // we don't have a connection available to us so we have to create one.
            final Consumer<Connection> onSuccess = c -> {
                final ConnectionEndpointId cid = c.id().getRemoteConnectionEndpointId();
                if (c.isTCP()) {
                    connections.put(cid, c);
                }
                c.send(IOEvent.create(c, proxyMsg));
            };

            connect(transport, remoteAddress, onSuccess);
        }
    }

    /**
     * Calculate the next hop. In SIP, you can specify the path through the network you wish the
     * message to go and this is expressed through Route-headers and the request-uri.
     *
     * Essentially, you check if there are {@link RouteHeader}s present, and if so, the top-most
     * {@link RouteHeader} is where you will proxy this message to and otherwise you will use the
     * request-uri as your target.
     *
     * Of course, you also need to check whether perhaps you are the ultimate target but we will
     * ignore this for now. This is a simple proxy and if you send us bad traffic, bad things will
     * happen :-)
     *
     * @param request
     * @return
     */
    private SipURI getNextHop(final SipRequest request) {

        // normally you also need to check whether this route is
        // pointing to you and it it is you have to "consume" it
        // and look at the next one. As it stands now, if this
        // route is pointing to us and we will use it as the next
        // hop we will of course create a loop. For now, we will
        // ignore this.
        final RouteHeader route = request.getRouteHeader();
        if (route != null) {
            return route.getAddress().getURI().toSipURI();
        }

        return request.getRequestUri().toSipURI();
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        try {
            final IOEvent event = (IOEvent) evt;
            if (event.isConnectionIOEvent()) {
                processConnectionEvent(event.toConnectionIOEvent());
            }
        } catch (final ClassCastException e) {
            // guess this is not an IOEvent, which is the only thing we handle
            // so just forward it...
            ctx.fireUserEventTriggered(evt);
        }
    }

    /**
     * Whenever we get a new connection we will add that to our internal storage
     * of known connections. Whenever we get a disconnect, we will remove it.
     *
     * @param event
     */
    private void processConnectionEvent(final ConnectionIOEvent event) {
        final Connection connection = event.connection();
        if (event.isConnectionInactiveIOEvent()) {
            connections.remove(connection.id().getRemoteConnectionEndpointId());
        } else if (event.isConnectionActiveIOEvent()) {
            connections.put(connection.id().getRemoteConnectionEndpointId(), connection);
        }
    }

    /**
     *
     * @param ip
     * @param port
     * @return
     */
    public Future<Connection> connect(final Transport transport, final InetSocketAddress remoteAddress, final Consumer<Connection> consumer) {
        final CompletableFuture<Connection> f =
                transport.isTCP() ? connectTCP(remoteAddress) : connectUDP(remoteAddress);
        f.thenAccept(consumer);
        return f;
    }

    /**
     * Get a connection based on the {@link ConnectionId}.
     *
     * @param id
     * @param consumer
     * @return
     */
    public Future<Connection> connect(final ConnectionId id, final Consumer<Connection> consumer) {
        final Connection connection = connections.get(id.getRemoteConnectionEndpointId());
        if (connection != null) {
            final CompletableFuture<Connection> f = CompletableFuture.completedFuture(connection);
            f.thenAccept(consumer);
            return f;
        }

        return connect(id.getProtocol(), id.getRemoteAddress(), consumer);
    }

    private CompletableFuture<Connection> connectUDP(final InetSocketAddress remoteAddress) {
        return CompletableFuture.completedFuture(new UdpConnection(udpChannel, remoteAddress));
    }

    private CompletableFuture<Connection> connectTCP(final InetSocketAddress remoteAddress) {
        final CompletableFuture<Connection> f = new CompletableFuture<>();
        final ChannelFuture channelFuture = tcpBootstrap.connect(remoteAddress);
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

    /**
     * Very basic helper method for loading a yaml file and with the help of jackson, de-serialize it
     * to a configuration object.
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static SimpleProxyConfiguration loadConfiguration(final String file) throws Exception {
        final InputStream stream = new FileInputStream(file);
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(stream, SimpleProxyConfiguration.class);
    }


    public static void main(final String ... args) throws Exception {

        // just to keep things simple. Normally you want to use a CLI framework
        if (args.length == 0) {
            throw new IllegalArgumentException("You must specify the configuration file to load");
        }

        final SimpleProxyConfiguration config = loadConfiguration(args[0]);

        final InetSocketAddress socketAddress = new InetSocketAddress(config.getListenAddress(), config.getListenPort());
        final Proxy proxy = new Proxy(socketAddress, config);

        // Create the UDP client and server stack
        final EventLoopGroup udpGroup = new NioEventLoopGroup();
        final Bootstrap b = new Bootstrap();
        b.group(udpGroup)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(final DatagramChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new SipMessageDatagramDecoder());
                        pipeline.addLast("encoder", new SipMessageDatagramEncoder());
                        pipeline.addLast("handler", proxy);
                    }
                });

        // Create the TCP server stack
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        final EventLoopGroup bossGroup = new NioEventLoopGroup();
        final EventLoopGroup workerGroup = new NioEventLoopGroup();

        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(final SocketChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new SipMessageStreamDecoder());
                        pipeline.addLast("encoder", new SipMessageStreamEncoder());
                        pipeline.addLast("handler", proxy);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

        // Create the TCP client stack
        final Bootstrap tcpBootstrap = new Bootstrap();
        tcpBootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new SipMessageStreamDecoder());
                        pipeline.addLast("encoder", new SipMessageStreamEncoder());
                        pipeline.addLast("handler", proxy);
                    }
                });

        final Channel udpChannel = b.bind(socketAddress).sync().channel();
        serverBootstrap.bind(socketAddress).sync().channel();
        proxy.setTcpStack(tcpBootstrap);
        proxy.setUdpChannel(udpChannel);
        udpChannel.closeFuture().await();
    }
}
