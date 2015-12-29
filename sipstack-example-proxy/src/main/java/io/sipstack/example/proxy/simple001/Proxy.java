package io.sipstack.example.proxy.simple001;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.header.RouteHeader;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.SipMessageDatagramDecoder;
import io.sipstack.netty.codec.sip.SipMessageDatagramEncoder;
import io.sipstack.netty.codec.sip.UdpConnection;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.netty.codec.sip.event.SipMessageIOEvent;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;

/**
 * This is a very simple proxy example written on top of the raw
 * netty + pkts.io support as exposed through sipstack.io.
 *
 * @author jonas@jonasborjesson.com
 */
@Sharable
public class Proxy extends SimpleChannelInboundHandler<SipMessageIOEvent> {

    private final SimpleProxyConfiguration config;

    private Channel udpChannel;

    public Proxy(final SimpleProxyConfiguration config) {
        this.config = config;
        System.out.println("Starting " + config.getName());
    }

    /**
     * This is ugly, and a catch 22, but we need to know the channel so
     * that we can create outbound connections in order to send
     * messages there.
     *
     * @param udpChannel
     */
    public void setUdpChannel(final Channel udpChannel) {
        this.udpChannel = udpChannel;
    }

    /**
     * Create a new connection, which we technically do not
     * need to do since in this example we are assuming UDP
     * and as such, there is no "connection" per se, plus at
     * least for responses, it would have been enough to re-use
     * the same channel object as we already registered with
     * in {@link Proxy#setUdpChannel(Channel)} but in the next
     * few examples you will understand why.
     *
     * @param ip
     * @param port
     * @return
     */
    public Connection connect(final Buffer ip, final int port) {
        final InetSocketAddress remoteAddress = new InetSocketAddress(ip.toString(), port);
        return new UdpConnection(udpChannel, remoteAddress);
    }


    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageIOEvent event) throws Exception {
        final SipMessage msg = event.message();

        if (msg.isRequest()) {
            final SipURI next = getNextHop(msg.toRequest());
            proxyTo(next, msg.toRequest());
        } else {
            // Responses follow via headers to just pop the top-most via header
            // and then use the information found in the second via.
            // Of course, in a real stack we should check that the top-most via
            // actually pointed to us but this is a simple example so we'll continue
            // to live in happy land...
            final SipResponse response = msg.toResponse().copy().withPoppedVia().build();
            final ViaHeader via = response.getViaHeader();
            final Connection connection = connect(via.getHost(), via.getPort());
            connection.send(IOEvent.create(connection, response));
        }
    }

    /**
     * Whenever we proxy a request we must also add a Via-header, which essentially says that the
     * request went "via this network address using this protocol". The {@link ViaHeader}s are used
     * for responses to find their way back the exact same path as the request took.
     *
     * @param destination
     * @param msg
     */
    private void proxyTo(final SipURI destination, final SipRequest msg) {
        final Connection connection = connect(destination.getHost(), destination.getPort());

        // Create a new Via-header that points back to us
        final ViaHeader via = ViaHeader.withHost(config.getListenAddress())
                .withPort(config.getListenPort())
                .withTransportUdp()
                .withBranch()
                .withRPortFlag()
                .build();

        // Create the proxy request, where we will add our Via-header
        // as well as decrement the Max-Forwards header.
        final SipMessage proxyMsg = msg.copy()
                .withTopMostViaHeader(via)
                .onMaxForwardsHeader(max -> max.decrement())
                .build();

        // Because the raw network support provided by sipstack.io expects
        // IOEvents we will have to wrap the actual SIP message in one
        // of those events before sending it off. Now, as we build on
        // this example eventually this will go away since we will be
        // adding more higher level support to this proxy example.
        // But, again, this is an example of how to use the most low-level
        // support from sipstack.io to get a SIP Proxy going so it is
        // what it is...
        connection.send(IOEvent.create(connection, proxyMsg));
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

        final Proxy proxy = new Proxy(config);

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

        final InetSocketAddress socketAddress = new InetSocketAddress(config.getListenAddress(), config.getListenPort());
        final Channel udpChannel = b.bind(socketAddress).sync().channel();
        proxy.setUdpChannel(udpChannel);
        udpChannel.closeFuture().await();
    }
}
