package io.sipstack.example.transaction;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.HashWheelScheduler;
import io.sipstack.actor.InternalScheduler;
import io.sipstack.config.NetworkInterfaceConfiguration;
import io.sipstack.config.SipConfiguration;
import io.sipstack.net.NettyNetworkLayer;
import io.sipstack.net.NetworkLayer;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.SystemClock;
import io.sipstack.netty.codec.sip.Transport;
import io.sipstack.transaction.event.TransactionEvent;
import io.sipstack.transaction.impl.DefaultTransactionLayer;
import io.sipstack.transport.impl.DefaultTransportLayer;

import java.util.List;

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
public class UAS extends SimpleChannelInboundHandler<TransactionEvent> {

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final TransactionEvent event) throws Exception {
        if (event.isSipTransactionEvent()) {
            final SipMessage msg = event.toSipTransactionEvent().message();
            if (msg.isRequest() && !msg.isAck()) {
                final SipResponse response = msg.createResponse(200).build();
                event.transaction().send(response);
            }
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
    private static NetworkLayer configureNetworkStack(final UAS uas, final String ip, final int port) {

        // Create a new configuration object. In a real scenario you
        // want to read the values off of a yaml file but for this example
        // this is good enough.
        final SipConfiguration sipConfig = new SipConfiguration();

        // configure a listening address. Both udp and tcp...
        // again, normally you would define this in your configuration
        // file so there would be no need to do this programmatically
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

        // The transport and transaction layers both need a scheduler so that they
        // can schedule events and everything in sipstack.io is using the clock
        // interface for getting the current time (easier to unit test that way)
        // so create those and pass them in...
        final InternalScheduler scheduler = new HashWheelScheduler();
        final Clock clock = new SystemClock();

        // Any SIP stack really needs a transport layer, which is responsible
        // for maintaining connections etc. This layer MUST be the first one
        // in our netty handler chain.
        final DefaultTransportLayer transportLayer = new DefaultTransportLayer(sipConfig.getTransport(), clock, scheduler);
        networkBuilder.withHandler("transport-layer", transportLayer);

        // The transaction layer is responsible for transaction
        // management and is typically always present in a SIP stack.
        // This layer MUST come after the Transport Layer but BEFORE
        // the application layer, which is the one we will build.
        final DefaultTransactionLayer transactionLayer = new DefaultTransactionLayer(transportLayer, clock, scheduler, sipConfig.getTransaction());
        networkBuilder.withHandler("transaction-layer", transactionLayer);

        // Finally, add your own Netty handler to the chain, which must be the last
        // out of them all.
        networkBuilder.withHandler("my-application", uas);

        // Build the network
        final NettyNetworkLayer server = networkBuilder.build();

        // catch 22. The transport layer needs a reference to the network layer
        // and the network layer needs transport layer to be part of the
        // netty chain. Not pretty but couldn't figure out a better way
        // but if you do, please let me know!
        transportLayer.useNetworkLayer(server);

        return server;
    }

    public static void main(final String ... args) throws Exception {
        final UAS uas = new UAS();
        NetworkLayer stack = configureNetworkStack(uas, "127.0.0.1", 5060);
        stack.start();
        stack.sync();
    }

}
