package io.sipstack.transport.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.actor.ActorContext;
import io.sipstack.actor.GenericSingleContext;
import io.sipstack.actor.InternalScheduler;
import io.sipstack.actor.SingleContext;
import io.sipstack.config.TransportLayerConfiguration;
import io.sipstack.core.SipStack;
import io.sipstack.core.SipTimerListener;
import io.sipstack.event.SipTimerEvent;
import io.sipstack.net.InboundOutboundHandlerAdapter;
import io.sipstack.net.ListeningPoint;
import io.sipstack.net.NetworkLayer;
import io.sipstack.netty.codec.sip.*;
import io.sipstack.netty.codec.sip.event.ConnectionIOEvent;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowFuture;
import io.sipstack.transport.FlowId;
import io.sipstack.transport.TransportLayer;
import io.sipstack.transport.event.FlowEvent;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The {@link DefaultTransportLayer} is responsible for maintaining {@link Flow}s, which represents
 * the connection between two endpoints.
 *
 * This layer also acts as the bridge between Netty and the rest of the {@link SipStack} (may change)
 *
 * @author jonas@jonasborjesson.com
 */
public class DefaultTransportLayer extends InboundOutboundHandlerAdapter implements TransportLayer, SipTimerListener {

    private final TransportLayerConfiguration config;

    // TODO: need to configure this...
    private final FlowStore flowStore;

    private final Clock clock;

    private final InternalScheduler scheduler;

    /**
     * The {@link DefaultTransportLayer} is the only one that actually
     * cares about the underlying network since it is the only
     * one that actually will manage connections etc. The rest
     * of the stack will either only see flows or simply just
     * asks to send a message and this layer will figure out
     * where the message is actually supposed to go.
     */
    private NetworkLayer network;

    public DefaultTransportLayer(final TransportLayerConfiguration config,
                                 final Clock clock,
                                 final InternalScheduler scheduler) {
        this.config = config;
        flowStore = new DefaultFlowStore(config.getFlow());
        this.clock = clock;
        this.scheduler = scheduler;
    }

    public void useNetworkLayer(final NetworkLayer network) {
        this.network = network;
    }

    @Override
    public void read(final ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
        // Whenever we are done with read we will flush
        // any potential messages that need to go out.
        // Hence, never ever call writeAndFlush from
        // any place in the handler chain
        ctx.flush();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        try {
            final IOEvent event = (IOEvent)msg;

            if (!event.isSipMessageIOEvent()) {
                System.err.println("Ok, don't handle any other event that sip right now");
                return;
            }

            System.err.println("Got an IOEvent");

            final Connection connection = event.connection();
            final FlowActor actor = flowStore.ensureFlow(connection);
            // TODO: invoke transaction here

            // final SipMessage sipMsg = event.toSipMessageIOEvent().message();
            // final FlowEvent flowEvent = sipMsg.isRequest() ? FlowEvent.create(flow, sipMsg.toRequest()) :
                    // FlowEvent.create(flow, sipMsg.toResponse());

            // TODO: invoke FlowActor and then we may potentially be sending this
            // TODO: upstream/downstream. The downstream would e.g. be because we received a PING
            // TODO: and should be sending a PONG back.

            // ctx.fireChannelRead(flowEvent);
        } catch (final ClassCastException e) {
            e.printStackTrace();;
        }
    }

    /**
     * From ChannelInboundHandler
     */
    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        try {
            final IOEvent event = (IOEvent) evt;
            if (event.isConnectionIOEvent()) {
                processConnectionIOEvent(ctx, event.toConnectionIOEvent());
            }
        } catch (final ClassCastException e) {
            // guess this is not an IOEvent, which is the only thing we handle
            // so just forward it...
            ctx.fireUserEventTriggered(evt);
        }
    }

    /**
     * Whenever a connection is established or torn down we will be getting
     * {@link ConnectionIOEvent} representing that connection event.
     *
     * @param event
     */
    private void processConnectionIOEvent(final ChannelHandlerContext ctx, final ConnectionIOEvent event) {
        final Connection connection = event.connection();

        // TODO: are these the correct events that creates a flow? Should e.g. ConnectionInactive result
        // in a new flow gets created just to have it removed right away?
        FlowActor actor = null;
        if (event.isConnectionActiveIOEvent() || event.isConnectionBoundIOEvent()) {
            actor = flowStore.ensureFlow(connection);
        } else {
            actor = flowStore.get(FlowId.create(connection.id()));
        }

        if (actor != null) {
            // invoke actor
            // Currently the actor only accepts FlowEvents, is this really correct?
            // At the end of the day, it really consumes the raw events from the
            // low level stack. However, when we push events upstream we have to
            // create a FlowEvent so that object also encapsulates the actual flow
            invokeActor(ctx, actor, event);
        }
    }

    private void invokeActor(final ChannelHandlerContext channelCtx, final FlowActor actor, final IOEvent event) {
        try {
            final GenericSingleContext<IOEvent> ctx = new GenericSingleContext<IOEvent>(clock, channelCtx, scheduler, actor.id(), this);
            actor.onReceive(ctx, event);

            // always favor downstream
            ctx.downstream().ifPresent(e -> {
                System.err.println("TODO: Received a downstream event from the Flow Actor.");
            });

            ctx.upstream().ifPresent(e -> {
                System.err.println("TODO: Received an upstream event from the Flow Actor.");
            });
        } catch (final Throwable t) {
            // TODO: need to decide what to do.
            // Did the flow die due to this?
            // If yes, should we try and resurrect it? Or just
            // tear down the connection and inform elements upstream?
            // I guess that would be ok but it seems like we should do everything
            // in our power to actually re-create the flow but if that doesn't work
            // then I guess we at some point have to to let the upstreams know
            t.printStackTrace();
        }
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        System.err.println("connecting to " + remoteAddress + " from local " + localAddress);
        ctx.connect(remoteAddress, localAddress, promise);
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        System.err.println("disconnecting...");
        ctx.disconnect(promise);
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        final FlowEvent event = (FlowEvent)msg;
        if (event.isSipFlowEvent()) {
            final SipMessage sip = event.toSipFlowEvent().message();

            // TODO: what if the flow doesn't have a connection present, such as in
            // the case of faulty flows (broken etc)? We probably should generate a
            // transport failed event of some sort.
            final InternalFlow flow = (InternalFlow)event.flow();
            flow.connection().ifPresent(c -> ctx.write(IOEvent.create(c, sip), promise));
        }
    }

    @Override
    public Flow.Builder createFlow(final String host) {
        PreConditions.ensureNotEmpty("You must specify the host to connect to", host);
        return new FlowBuilder(host);
    }

    @Override
    public void onTimeout(final SipTimerEvent timer) {
        System.err.println("Received a timeout event");
    }

    private class FlowBuilder implements Flow.Builder {

        private Consumer<Flow> onSuccess;
        private Consumer<Flow> onFailure;
        private Consumer<Flow> onCancelled;
        private String host;
        private int port;
        private Transport transport;

        private FlowBuilder(final String host) {
            this.host = host;
        }

        @Override
        public Flow.Builder withPort(final int port) {
            this.port = port;
            return this;
        }

        @Override
        public Flow.Builder withTransport(final Transport transport) {
            this.transport = transport;
            return this;
        }

        @Override
        public Flow.Builder onSuccess(final Consumer<Flow> consumer) {
            this.onSuccess = consumer;
            return this;
        }

        @Override
        public Flow.Builder onFailure(final Consumer<Flow> consumer) {
            this.onFailure = consumer;
            return this;
        }

        @Override
        public Flow.Builder onCancelled(Consumer<Flow> consumer) {
            this.onCancelled = consumer;
            return this;
        }

        @Override
        public FlowFuture connect() throws IllegalArgumentException {
            final int port = this.port == -1 ? defaultPort() : this.port;

            // TODO: need to ensure that the host is an actual IP-address
            final Transport transportToUse = transport == null ? Transport.udp : transport;
            final InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
            final Optional<ListeningPoint> lpToUse = network.getListeningPoint(transportToUse);

            // if we cannot find an appropriate ListeningPoint then we are dead. Return
            // a failed future.
            if (!lpToUse.isPresent()) {
                // TODO: create a failed FlowFuture and complete it right away. For now, exception...
                // TODO: or should we re-throw exception? I think we should stick with one thing so lets
                // go for a failed future.
                throw new RuntimeException("Guess we don't support the transport or something. Create failed FlowFuture here instead");
            }

            final ListeningPoint lp = lpToUse.get();
            final ConnectionId connectionId =
                    ConnectionId.create(transportToUse, lp.getLocalAddress(), remoteAddress);
            final FlowId flowId = FlowId.create(connectionId);
            final FlowActor actor = flowStore.get(flowId);
            FlowFuture flowFuture = null;
            if (actor != null) {
                flowFuture = new SuccessfulFlowFuture();
                invokeCallbackDirectly(actor.flow(), onSuccess);
            } else if (connectionId.isReliableTransport()) {
                // TODO: connect directly via the ListeningPoint instead.
                // TODO: actually need to store the future...
                final ChannelFuture future = network.connect(remoteAddress, transportToUse);
                final FlowFutureImpl flowFutureImpl = new FlowFutureImpl(flowStore, future, onSuccess, onFailure, onCancelled);
                future.addListener(flowFutureImpl);
                flowFuture = flowFutureImpl;
            } else {
                // for unreliable transports, we will simply create a connection as is without
                // actually "connecting".
                final Connection connection = new UdpConnection(lp.getChannel(), remoteAddress);
                final FlowActor newActor = flowStore.ensureFlow(connection);

                flowFuture = new SuccessfulFlowFuture();
                invokeCallbackDirectly(newActor.flow(), onSuccess);
            }
            return flowFuture;
        }

        private void invokeCallbackDirectly(final Flow flow, final Consumer<Flow> callback) {
            try {
                callback.accept(flow);
            } catch (final Throwable t) {
                // TODO: do something about this... guess we should propagate the exception up the chain or something
                t.printStackTrace();
            }
        }

        /**
         * The default port should probably be coming from somewhere else.
         * @return
         */
        private int defaultPort() {
            if (transport == null || transport == Transport.udp || transport == Transport.tcp) {
                return 5060;
            }

            if (transport == Transport.tls) {
                return 5061;
            }

            if (transport == Transport.ws) {
                return 5062;
            }

            if (transport == Transport.wss) {
                return 5063;
            }

            return 5060;
        }
    }
}
