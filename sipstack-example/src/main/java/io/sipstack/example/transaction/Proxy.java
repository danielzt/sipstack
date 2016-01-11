package io.sipstack.example.transaction;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.pkts.packet.sip.Transport;
import io.sipstack.config.NetworkInterfaceConfiguration;
import io.sipstack.config.SipConfiguration;
import io.sipstack.net.netty.NettyNetworkLayer;
import io.sipstack.net.NetworkLayer;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.SystemClock;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionLayer;
import io.sipstack.transaction.event.TransactionEvent;
import io.sipstack.transaction.event.TransactionLifeCycleEvent;
import io.sipstack.transaction.impl.DefaultTransactionLayer;
import io.sipstack.transport.impl.DefaultTransportLayer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The examples under io.sipstack.example.netty.sip are all using the raw sip support
 * but doesn't offer up much of a "real" SIP stack. Essentially, that layer only provides
 * the basic SIP syntax layer (framing & parsing) but normally you want at the very least
 * have the SIP Transport & SIP Transaction Layer in place so that you can build something
 * more real. This example configures the stack with those layers.
 *
 * @author jonas@jonasborjesson.com
 */
@Sharable
public class Proxy extends SimpleChannelInboundHandler<TransactionEvent> {

    private final TransactionLayer transactionLayer;
    private final String remoteIp;
    private final int remotePort;
    private final Map<TransactionId, Transaction> outstandingTransactions = new ConcurrentHashMap<>(1024);

    public Proxy(final TransactionLayer transactionLayer, final String remoteIp, final int remotePort) {
        this.transactionLayer = transactionLayer;
        this.remoteIp = remoteIp;
        this.remotePort = remotePort;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final TransactionEvent event) throws Exception {
        throw new RuntimeException("TODO: re-writing everything again");
        /*
        if (event.isSipTransactionEvent()) {
            final Transaction transaction = event.transaction();
            outstandingTransactions.put(transaction.id(), transaction);
            final SipMessage msg = event.toSipTransactionEvent().message();

            transaction.onResponse(r -> r.transaction.send());
            transaction.onFlowFailure();
            transaction.onRetransmit();
            transaction.onTerminated();

            if (msg.isRequest()) {
                final SipRequest request = msg.toRequest();
                transactionLayer.createFlow(remoteIp)
                        .withPort(remotePort)
                        .withTransport(Transport.udp)
                        .onSuccess(f -> {
                            // TODO: this should be added automatically by the
                            // transport layer.
                            final ViaHeader via = ViaHeader.with().host("127.0.0.1").port(5060).transportUDP().branch(ViaHeader.generateBranch()).build();
                            request.addHeaderFirst(via);
                            final ClientTransaction ct = transactionLayer.newClientTransaction(f, request);
                            ct.onresponse(r -> apa; ctx.fireChannelRead());
                            ct.onTerminated();
                            outstandingTransactions.put(ct.id(), ct);
                            ct.start();
                        })
                        .onFailure(f -> System.err.println("What, the flow failed!!!"))
                        .onCancelled(f -> System.err.println("What, the flow was cancelled!!!"))
                        .connect();

            } else if (msg.isResponse()) {
                final SipResponse response = msg.toResponse();
                final ViaHeader via = response.popViaHeader();
                final TransactionId id = TransactionId.create(response);
                final Transaction serverTransaction = outstandingTransactions.get(id);
                serverTransaction.send(response);
            }
        } else if (event.isTransactionLifeCycleEvent()) {
            processTransactionLifeCycleEvent(event.toTransactionLifeCycleEvent());
        }
        */
    }

    private void processTransactionLifeCycleEvent(final TransactionLifeCycleEvent event) {
        if (event.isTransactionTerminatedEvent()) {
            outstandingTransactions.remove(event.transaction().id());
            // System.err.println("yay, transaction died!!! " + outstandingTransactions.size() + " transactions outstanding");
        }
    }

    /**
     * Helper method for setting up the network stack, which in turn makes use of a network builder.
     *
     * A few things to note:
     * <ul>
     *     <li></li>
     *     <li></li>
     *     <li></li>
     *     <li></li>
     * </ul>
     *
     * @return
     */
    private static NetworkLayer configureNetworkStack(final String ip, final int port) {

        // Create a new configuration object. In a real scenario you
        // want to read the values off of a yaml file but for this example
        // this is good enough.
        final SipConfiguration sipConfig = new SipConfiguration();

        // configure a listening address. Both udp and tcp...
        sipConfig.listen(ip, port, Transport.udp, Transport.tcp);

        // Somewhat silly since we just configured it but grab the configured
        // network interfaces and then call the NettyNetworkLayer to get a network
        // builder...
        final List<NetworkInterfaceConfiguration> ifs = sipConfig.getNetworkInterfaces();
        final NettyNetworkLayer.Builder networkBuilder = NettyNetworkLayer.with(ifs);

        // Ok, that was just configuring the basics as far as the
        // listening ports etc. Also, if you have a look at the NettyNetworkLayer.builder
        // you will see that it will actually also insert the netty handler for framing
        // and parsing the incoming byte streams...

        // Any SIP stack really needs a transport layer, which is responsible
        // for maintaining connections etc. This layer MUST be the first one
        // in our netty handler chain.
        final Clock clock = new SystemClock();
        final DefaultTransportLayer transportLayer = new DefaultTransportLayer(sipConfig.getTransport(), clock, null);
        networkBuilder.withHandler("transport-layer", transportLayer);

        // The transaction layer is responsible for transaction
        // management and is typically always present in a SIP stack.
        // This layer MUST come after the Transport Layer but BEFORE
        // the application layer, which is the one we will build.
        final DefaultTransactionLayer transactionLayer = new DefaultTransactionLayer(transportLayer, sipConfig.getTransaction());
        networkBuilder.withHandler("transaction-layer", transactionLayer);

        // Finally, add your own Netty handler to the chain, which must be the last
        // out of them all.
        final Proxy proxy = new Proxy(transactionLayer, "127.0.0.1", 5070);
        networkBuilder.withHandler("my-application", proxy);

        // Build the network
        final NettyNetworkLayer server = networkBuilder.build();

        // catch 22. The transport layers needs a reference to the network layer
        // and the network layer needs transport layer to be part of the
        // netty chain.
        transportLayer.useNetworkLayer(server);

        return server;
    }

    public static void main(final String ... args) throws Exception {
        NetworkLayer stack = configureNetworkStack("127.0.0.1", 5060);
        stack.start();
        stack.sync();
    }

}
