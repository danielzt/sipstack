package io.sipstack.example.proxy.simple005;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.Transport;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.header.RouteHeader;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.actor.HashWheelScheduler;
import io.sipstack.actor.InternalScheduler;
import io.sipstack.config.NetworkInterfaceConfiguration;
import io.sipstack.config.NetworkInterfaceDeserializer;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.net.NetworkLayer;
import io.sipstack.net.netty.NettyNetworkLayer;
import io.sipstack.netty.codec.sip.*;
import io.sipstack.netty.codec.sip.event.ConnectionIOEvent;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.netty.codec.sip.event.SipMessageIOEvent;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowFuture;
import io.sipstack.transport.TransportLayer;
import io.sipstack.transport.event.FlowEvent;
import io.sipstack.transport.impl.DefaultTransportLayer;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * in Proxy 004 we utilized the Network Layer to aid us in setting up
 * the netty network layer and also to help us create new connections.
 * However, we still had to maintain those connections ourselves and
 * we even didn't solve the issue of when to kill those UDP Connections.
 *
 * In this example we add the Transport Layer, which is a very important
 * part of any network stack. It's primary purpose is to do connection
 * management.
 *
 * @author jonas@jonasborjesson.com
 */
@Sharable
public class Proxy extends SimpleChannelInboundHandler<FlowEvent> {

    private final SimpleProxyConfiguration config;

    private TransportLayer transport;

    public Proxy(final SimpleProxyConfiguration config) {
        this.config = config;
        System.out.println("Starting " + config.getName());
    }

    public void setTransportLayer(final TransportLayer transport) {
        this.transport = transport;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FlowEvent event) throws Exception {
        final Flow flow = event.flow();
        if (event.isSipFlowEvent()) {
            final SipMessage msg = event.toSipFlowEvent().message();

            if (msg.isRequest()) {
                final SipURI next = getNextHop(msg.toRequest());
                proxyTo(next, flow, msg.toRequest());
            } else {
                // Responses follow via headers to just pop the top-most via header
                // and then use the information found in the second via.
                // Of course, in a real stack we should check that the top-most via
                // actually pointed to us but this is a simple example so we'll continue
                // to live in happy land...
                final ConnectionId connectionId = ConnectionId.decode(msg.getViaHeader().getParameter("flow"));
                final Consumer<Flow> onSuccess = f -> {
                    final SipResponse response = msg.toResponse().copy().withPoppedVia().build();
                    f.send(response);
                };
                connect(connectionId, onSuccess);
            }
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
    private void proxyTo(final SipURI destination, final Flow flow, final SipRequest msg) {

        // Create a new Via-header that points back to us
        final Transport transport = destination.getTransportParam().orElse(Transport.udp);

        // Create the proxy request, where we will add our Via-header
        // as well as decrement the Max-Forwards header.

        final String remoteHost = destination.getHost().toString();
        final int remotePort = destination.getPort();

        final InetSocketAddress remoteAddress = new InetSocketAddress(remoteHost, remotePort);
        final ConnectionEndpointId id = ConnectionEndpointId.create(transport, remoteAddress);

        final Consumer<Flow> onSuccess = f -> {
            final String localAddress = f.getLocalIpAddress();
            final int localPort = f.getLocalPort();
            final ViaHeader via = ViaHeader.withHost(localAddress)
                    .withPort(localPort)
                    .withTransport(f.getTransport())
                    .withBranch()
                    .withRPortFlag()
                    // As with a connection, we want to use the same flow coming back again
                    .withParameter(Buffers.wrap("flow"), flow.id().encode())
                    .build();

            final SipMessage proxyMsg = msg.copy()
                    .withTopMostViaHeader(via)
                    .onViaHeader((index, v) -> {
                        if (index == 1) {
                            // Add the received and rport information to the
                            // previous top-most Via, which is now our second
                            // Via on the list and since we start counting at
                            // zero, the index for the second via is 1...
                            v.withReceived(flow.getRemoteIpAddressAsBuffer()).withRPort(flow.getRemotePort());
                        }
                    })
                    .onMaxForwardsHeader(max -> max.decrement())
                    .build();
            f.send(proxyMsg);
        };

        connect(id, onSuccess);
    }

    public CompletableFuture<Flow> connect(final ConnectionEndpointId id, final Consumer<Flow> consumer) {
        final Consumer<Flow> onCancelled = f -> System.err.println("Flow was cancelled: " + f);
        final Consumer<Flow> onFailure = f -> System.err.println("Flow failed: " + f.getFailureCause());
        return transport.createFlow(id.getIpAddress())
                .withPort(id.getPort())
                .withTransport(id.getProtocol())
                .onSuccess(consumer)
                .onFailure(onFailure)
                .onCancelled(onCancelled)
                .connect();
    }

    /**
     * Get a connection based on the {@link ConnectionId}.
     *
     * @param id
     * @param consumer
     * @return
     */
    public CompletableFuture<Flow> connect(final ConnectionId id, final Consumer<Flow> consumer) {
        return connect(id.getRemoteConnectionEndpointId(), consumer);
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
     * Note: for UDP "connections", this simple example will actually leak
     * connections because there is no mechanism that keeps track of if a UDP
     * "connection" should be killed. As we will see in other examples, this
     * is going to be handled by the transport layer in the "full" example.
     *
     * @param event
     */
    private void processConnectionEvent(final ConnectionIOEvent event) {
        System.err.println("Proxy005: received a connection event " + event);
        /*
        final Connection connection = event.connection();
        if (event.isConnectionInactiveIOEvent()) {
            flows.remove(connection.id().getRemoteConnectionEndpointId());
        } else if (event.isConnectionActiveIOEvent()) {
            flows.put(connection.id().getRemoteConnectionEndpointId(), connection);
        }
        */
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
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(NetworkInterfaceConfiguration.class, new NetworkInterfaceDeserializer());
        mapper.registerModule(module);
        return mapper.readValue(stream, SimpleProxyConfiguration.class);
    }

    public static void main(final String ... args) throws Exception {

        // just to keep things simple. Normally you want to use a CLI framework
        if (args.length == 0) {
            throw new IllegalArgumentException("You must specify the configuration file to load");
        }

        // We now extended our SimpleProxyConfiguration class with
        // the ability to also specify the network listening points, which
        // then we can use to configure the entire Netty stack.
        final SimpleProxyConfiguration config = loadConfiguration(args[0]);
        final Proxy proxy = new Proxy(config);
        final NettyNetworkLayer.Builder builder = NettyNetworkLayer.with(config.getNetworkInterfaces());

        // Transport layer is responsible for managing connections,
        // i.e. Flows.
        final TransportLayerConfiguration transportConfig = config.getTransportLayerConfiguration();
        final Clock clock = new SystemClock();
        final InternalScheduler scheduler = new HashWheelScheduler();
        final DefaultTransportLayer transportLayer = new DefaultTransportLayer(transportConfig, clock, scheduler);
        builder.withHandler("transport-layer", transportLayer);

        // Add our own proxy last, which is very important. It cannot come
        // before the above transport-layer.
        builder.withHandler("proxy", proxy);

        final NetworkLayer network = builder.build();
        transportLayer.useNetworkLayer(network);
        proxy.setTransportLayer(transportLayer);

        // bring all the configured listening points up...
        network.start();

        // wait until all listening points shuts down.
        network.sync();
    }
}
