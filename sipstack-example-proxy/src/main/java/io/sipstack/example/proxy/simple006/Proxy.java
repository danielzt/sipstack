package io.sipstack.example.proxy.simple006;

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
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.net.NetworkLayer;
import io.sipstack.net.netty.NettyNetworkLayer;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.netty.codec.sip.SystemClock;
import io.sipstack.netty.codec.sip.event.ConnectionIOEvent;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.transaction.ClientTransaction;
import io.sipstack.transaction.ServerTransaction;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionLayer;
import io.sipstack.transaction.event.SipTransactionEvent;
import io.sipstack.transaction.event.TransactionEvent;
import io.sipstack.transaction.impl.DefaultTransactionLayer;
import io.sipstack.transport.Flow;
import io.sipstack.transport.TransportLayer;
import io.sipstack.transport.event.FlowEvent;
import io.sipstack.transport.impl.DefaultTransportLayer;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * In Proxy 005 we added the Transport Layer so we didn't have to maintain
 * the flows (connections) ourselves however, we still had to deal with
 * re-transmission logic etc (even though we actually didn't implement it).
 * In SIP, the Transaction Layer is responsible for dealing with re-transmits
 * and that is what we will be adding into this example.
 *
 * @author jonas@jonasborjesson.com
 */
@Sharable
public class Proxy extends SimpleChannelInboundHandler<TransactionEvent> {

    private final SimpleProxyConfiguration config;

    /**
     * We will use the {@link TransportLayer} to create flows for us over
     * which can send out SIP messages.
     */
    private TransactionLayer transactionLayer;

    public Proxy(final SimpleProxyConfiguration config) {
        this.config = config;
        System.out.println("Starting " + config.getName());
    }

    public void setTransactionLayer(final TransactionLayer transactionLayer) {
        this.transactionLayer = transactionLayer;
    }

    /**
     * @param ctx
     * @param event
     * @throws Exception
     */
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final TransactionEvent event) throws Exception {
        final Transaction transaction = event.transaction();

        // We will only ever get a ServerTransaction passed up ONCE so when you see a server transaction
        // you should setup it up the way you want. This is of course unlike a ClientTransaction since
        // we may see many responses for that transaction we will get it passed up many times. Hence, a
        // ClientTransaction should be configured when you create it (if you care)
        if (transaction.isServerTransaction()) {
            final ServerTransaction serverTransaction = transaction.toServerTransaction();
        }

        if (event.isSipTransactionEvent()) {
            final SipTransactionEvent sipTransaction = event.toSipRequestTransactionEvent();
            final SipMessage msg = sipTransaction.message();
            final Flow flow = sipTransaction.transaction().flow();
            if (msg.isRequest()) {
                final SipURI next = getNextHop(msg.toRequest());
                proxyTo(next, flow, msg.toRequest());
            } else {
                final SipResponse response = msg.toResponse().copy().withPoppedVia().build();
                transaction.send(response);
            }

        } else if (event.isTransactionTerminatedEvent()) {
            System.out.println("Transaction terminated");
        }
                /*
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
        */
    }

    /**
     * Note, unlike in Proxy005 we no longer keep track of the flow we came in over
     * since that is part of the Transaction we maintain.
     *
     * @param destination
     * @param flow the flow over which we received this sip message.
     *                   We need this connection to stamp the correct information
     *                   in the Via-header that came in across.
     * @param msg
     */
    private void proxyTo(final SipURI destination, final Flow flow, final SipRequest msg) {

        // only build up the actual SIP request
        // if we successfully connected to the destination.
        final Consumer<Flow> onSuccess = otherFlow -> {

            // First, we need to build up our new Via header.
            // Remember, this flow that just successfully completed
            // and that we got back in this callback is a flow
            // from our local machine to the remote host. Hence, the Via-header
            // has to include our local information so that responses finds
            // their way back to us.
            final ViaHeader via = ViaHeader.withHost(otherFlow.getLocalIpAddress())
                    .withPort(otherFlow.getLocalPort())
                    .withTransport(otherFlow.getTransport())
                    .withBranch()
                    .withRPortFlag()
                    .build();

            final SipRequest proxyMsg = msg.copy()
                    .withTopMostViaHeader(via)
                    .onViaHeader((index, v) -> {
                        if (index == 1) {
                            // Add the received and rport information to the
                            // previous top-most Via, which is now our second
                            // Via on the list and since we start counting at
                            // zero, the index for the second via is 1...
                            // Note that you MUST use the flow over which we
                            // received the incoming SIP Request so don't confuse
                            // this with the 'otherFlow'. That flow points to the
                            // remote target to which we are about to proxy this request.
                            v.withReceived(flow.getRemoteIpAddressAsBuffer()).withRPort(flow.getRemotePort());
                        }
                    })
                    .onMaxForwardsHeader(max -> max.decrement())
                    .build();
            final ClientTransaction transaction = transactionLayer.newClientTransaction(otherFlow, proxyMsg);
            // otherFlow.send(proxyMsg);
            transaction.start();
        };

        connect(destination, onSuccess);
    }

    /**
     * Connect to the destination represented by the {@link SipURI}.
     *
     * @param destination
     * @param onSuccess
     * @return
     */
    public CompletableFuture<Flow> connect(final SipURI destination, final Consumer<Flow> onSuccess) {

        // Use the transport specified in the destination or use UDP if none.
        // Note, this is a simple example and breaks RFC3263, which dictates rules
        // for which transport to use. For now, we'll ignore that...
        final Transport transport = destination.getTransportParam().orElse(Transport.udp);
        final String remoteHost = destination.getHost().toString();
        final int remotePort = destination.getPort();
        return connect(transport, remoteHost, remotePort, onSuccess);
    }

    /**
     * Get a connection based on the {@link ConnectionId}. Remember, a {@link ConnectionId} is an
     * encoded local to remote address pair together with the transport to use between them.
     * So if you have a {@link ConnectionId} you in fact have everything you need to establish
     * a connection between those to points or just check to where it is pointing...
     *
     * @param id
     * @param consumer
     * @return
     */
    public CompletableFuture<Flow> connect(final ConnectionId id, final Consumer<Flow> onSuccess) {
        final Transport transport = id.getProtocol();
        final String remoteHost = id.getRemoteIpAddress();
        final int remotePort = id.getRemotePort();
        return connect(transport, remoteHost, remotePort, onSuccess);
    }

    /**
     * Connect to the given destination based on the supplied transport, remote host and port.
     *
     * @param transport
     * @param remoteHost
     * @param remotePort
     * @param onSuccess
     * @return
     */
    private CompletableFuture<Flow> connect(final Transport transport,
                                            final String remoteHost,
                                            final int remotePort,
                                            final Consumer<Flow> onSuccess) {

        // Obviously, you want to do something more than just printing
        // something to stderr but again, this is a simple example so
        // we don't care...
        final Consumer<Flow> onCancelled = f -> System.err.println("Flow was cancelled: " + f);
        final Consumer<Flow> onFailure = f -> System.err.println("Flow failed: " + f.getFailureCause());

        return transactionLayer.createFlow(remoteHost)
                .withPort(remotePort)
                .withTransport(transport)
                .onSuccess(onSuccess)
                .onFailure(onFailure)
                .onCancelled(onCancelled)
                .connect();
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
        System.err.println("Proxy006: received a connection event " + event);
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

        final TransactionLayerConfiguration tlConfig = config.getTransactionLayerConfiguration();
        final DefaultTransactionLayer transactionLayer = new DefaultTransactionLayer(transportLayer, clock, scheduler, tlConfig);
        builder.withHandler("transaction-layer", transactionLayer);

        // Add our own proxy last, which is very important. It cannot come
        // before the above layers.
        builder.withHandler("proxy", proxy);

        final NetworkLayer network = builder.build();
        transportLayer.useNetworkLayer(network);
        proxy.setTransactionLayer(transactionLayer);

        // bring all the configured listening points up...
        network.start();

        // wait until all listening points shuts down.
        network.sync();
    }
}
