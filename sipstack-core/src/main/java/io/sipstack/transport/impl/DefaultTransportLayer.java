package io.sipstack.transport.impl;

import gov.nist.javax.sip.message.SIPMessage;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.Transport;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.actor.GenericSingleContext;
import io.sipstack.actor.InternalScheduler;
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
import io.sipstack.netty.codec.sip.event.SipMessageBuilderIOEvent;
import io.sipstack.netty.codec.sip.event.SipRequestBuilderIOEvent;
import io.sipstack.transaction.impl.InviteClientTransactionActor;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowFuture;
import io.sipstack.transport.FlowId;
import io.sipstack.transport.TransportLayer;
import io.sipstack.transport.event.FlowEvent;
import io.sipstack.transport.event.FlowTerminatedEvent;
import io.sipstack.transport.event.SipBuilderFlowEvent;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

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
    private final FlowStorage flowStorage;

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
                                 final FlowStorage flowStorage,
                                 final InternalScheduler scheduler) {
        this.config = config;
        this.flowStorage = flowStorage;
        this.clock = clock;
        this.scheduler = scheduler;
    }

    public DefaultTransportLayer(final TransportLayerConfiguration config,
                                 final Clock clock,
                                 final InternalScheduler scheduler) {
        this(config, clock, new DefaultFlowStorage(config, clock), scheduler);
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

            final Connection connection = event.connection();
            final FlowActor actor = flowStorage.ensureFlow(connection);

            if (actor != null) {
                // invoke actor
                // Currently the actor only accepts FlowEvents, is this really correct?
                // At the end of the day, it really consumes the raw events from the
                // low level stack. However, when we push events upstream we have to
                // create a FlowEvent so that object also encapsulates the actual flow
                invokeActor(true, ctx, actor, event);
            }

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

        // There are a few events that will create a new flow so if
        // receive one of those then create a new flow. See the flow
        // as outlined in the {@link FlowActor}
        FlowActor actor = null;
        if (event.isConnectionOpenedIOEvent() || event.isConnectionBoundIOEvent()) {
            actor = flowStorage.ensureFlow(connection);
        } else {
            actor = flowStorage.get(connection.id());
        }

        if (actor != null) {
            // invoke actor
            // Currently the actor only accepts FlowEvents, is this really correct?
            // At the end of the day, it really consumes the raw events from the
            // low level stack. However, when we push events upstream we have to
            // create a FlowEvent so that object also encapsulates the actual flow
            invokeActor(true, ctx, actor, event);
        }
    }

    /**
     * Invoke a {@link FlowActor} for a particular event.
     *
     * @param upstream unlike many other actors, the flow actor doesn't have a natural concept of direction
     *                 (compare with e.g. {@link InviteClientTransactionActor}) so we have to pass in the direction
     *                 since we need to know that in case the actor just does a "forward" on the event it got.
     *                 Forward means simply to pass the event in the direction it came from.
     * @param channelCtx the netty channel context
     * @param actor the actual actor
     * @param event the event we will pass onto the actor.
     */
    private void invokeActor(final boolean upstream, final ChannelHandlerContext channelCtx, final FlowActor actor, final IOEvent event) {
        try {
            synchronized (actor) {
                final GenericSingleContext<IOEvent> ctx = new GenericSingleContext<IOEvent>(clock, channelCtx, scheduler, actor.id(), this);
                actor.onReceive(ctx, event);

                // always favor downstream
                ctx.downstream().ifPresent(e -> {
                    channelCtx.writeAndFlush(e);
                });

                ctx.forward().ifPresent(e -> {
                    if (upstream) {
                        // Remember that the transport layer will ONLY emit FlowEvents to the
                        // next layer. The entire architecture is based on that each layer transforms
                        // the incoming event to something "richer" as the event goes up the pipeline.
                        if (event.isSipMessageIOEvent()) {
                            final SipMessage sipMsg = event.toSipMessageIOEvent().message();
                            final Flow flow = actor.flow();
                            final FlowEvent flowEvent = FlowEvent.create(flow, sipMsg);
                            channelCtx.fireChannelRead(flowEvent);
                        }
                    } else {
                        channelCtx.write(e);
                    }
                });

                ctx.upstream().ifPresent(e -> {
                    System.err.println("TODO: Received an upstream event from the Flow Actor.");
                });

                if (actor.isTerminated()) {
                    flowStorage.remove(actor.flow().id());
                    actor.stop();
                    actor.postStop();
                    final FlowTerminatedEvent terminatedEvent = FlowTerminatedEvent.create(actor.flow());
                    channelCtx.fireChannelRead(terminatedEvent);

                    // Probably want to issue a life-cycle event regarding the flow
                    // Compare with the transaction life-cycle events
                }
            }

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
        try {
            final FlowEvent event = (FlowEvent)msg;
            final Flow flow = event.flow();
            final FlowActor actor = flowStorage.get(flow.id());
            if (actor != null) {

                if (event.isSipFlowEvent()) {
                    final SipMessage sipMsg = event.toSipFlowEvent().message();
                    final IOEvent ioEvent = IOEvent.create(actor.connection(), sipMsg);
                    invokeActor(false, ctx, actor, ioEvent);
                } else if (event.isSipBuilderFlowEvent()) {
                    final SipMessage.Builder<? extends SipMessage> builder = event.toSipBuilderFlowEvent().getBuilder();
                    final SipMessageBuilderIOEvent ioEvent = IOEvent.create(actor.connection(), builder);
                    invokeActor(false, ctx, actor, ioEvent);
                }
            }
        } catch (final ClassCastException e) {
            // TODO: signal something
            e.printStackTrace();;
        }
    }

    @Override
    public Flow.Builder createFlow(final String host) {
        PreConditions.ensureNotEmpty(host, "You must specify the host to connect to");
        return new FlowBuilder(host);
    }

    @Override
    public Flow.Builder createFlow(final InetSocketAddress remoteAddress) {
        PreConditions.ensureNotNull(remoteAddress, "You must specify the host to connect to");
        return new FlowBuilder(remoteAddress);
    }

    @Override
    public void onTimeout(final SipTimerEvent timer) {
        try {
            final ConnectionId id = (ConnectionId) timer.key();
            final FlowActor actor = flowStorage.get(id);
            if (actor != null) {
                final IOEvent event = io.sipstack.netty.codec.sip.event.SipTimerEvent.create(timer.timer());
                invokeActor(true, timer.ctx(), actor, event);
            }
        } catch (final ClassCastException e) {
            // TODO: log error and move on?
        }
    }

    private class FlowBuilder implements Flow.Builder {

        private Consumer<Flow> onSuccess;
        private Consumer<Flow> onFailure;
        private Consumer<Flow> onCancelled;
        private final String host;
        private final InetSocketAddress remoteAddress;
        private String networkInterfaceName;
        private int port;
        private Transport transport;

        private FlowBuilder(final String host) {
            this.host = host;
            this.remoteAddress = null;
        }

        private FlowBuilder(final InetSocketAddress remoteAddress) {
            this.remoteAddress = remoteAddress;
            this.host = null;
        }

        @Override
        public Flow.Builder withPort(final int port) {
            if (this.remoteAddress != null) {
                throw new IllegalArgumentException("You cannot specify port when you already "
                        + "have specified the remote address");
            }
            this.port = port;
            return this;
        }

        @Override
        public Flow.Builder withTransport(final Transport transport) {
            this.transport = transport;
            return this;
        }

        @Override
        public Flow.Builder withNetworkInterface(final String interfaceName) {
            this.networkInterfaceName = interfaceName;
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
        public CompletableFuture<Flow> connect() throws IllegalArgumentException {
            final int port = this.port == -1 ? defaultPort() : this.port;

            // TODO: need to ensure that the host is an actual IP-address and if not
            // then we should resolve it or perhaps we should force the user to actually
            // resolve it first so flows only connect to IP addresses?
            final Transport transportToUse = transport == null ? Transport.udp : transport;
            final InetSocketAddress remoteAddress = this.remoteAddress != null ?
                    this.remoteAddress :
                    new InetSocketAddress(host, port);

            // TODO: allow to choose the network interface to use as well.
            final ListeningPoint lp = network.getListeningPoint(transportToUse).orElseThrow(() ->
                    new RuntimeException("Guess we don't support the transport or something. "
                            + "Create failed FlowFuture here instead"));

            final ConnectionId connectionId =
                    ConnectionId.create(transportToUse, lp.getLocalAddress(), remoteAddress);

            // This is the key of the remote endpoint. We will use it to
            // lookup the flow because if this is e.g. TCP then if we were
            // to use our local address then we won't find a Flow since every time
            // we connect over TCP we will get a different local port and as such,
            // it will never ever match lp.getLocalAddress().
            final ConnectionEndpointId endpointId = ConnectionEndpointId.create(transportToUse, remoteAddress);
            final FlowActor actor = flowStorage.get(endpointId);
            if (actor != null) {
                final CompletableFuture<Flow> future = CompletableFuture.completedFuture(actor.flow());
                invokeCallbackDirectly(actor.flow(), onSuccess);
                return future;
            }

            /**
             * Function for converting
             */
            final BiFunction<Connection, Throwable, Flow> bfn = (c, t) -> {
                if (t != null) {
                    if (t instanceof CancellationException) {
                        return new FailureFlow(connectionId, (CancellationException)t);
                    }
                    return new FailureFlow(connectionId, t);
                }

                final Flow flow = flowStorage.ensureFlow(c).flow();
                return flow;
            };

            final Consumer<Flow> fn = f -> {
                if (f.isValid() && onSuccess != null) {
                    onSuccess.accept(f);
                } else if (f.isCancelled() && onCancelled != null) {
                    onCancelled.accept(f);
                } else if (f.isFailed() && onFailure != null) {
                    onFailure.accept(f);
                }
            };

            final CompletableFuture<Connection> connectionFuture = lp.connect(remoteAddress);
            final CompletableFuture<Flow> flowFuture = connectionFuture.handle(bfn);
            flowFuture.thenAccept(fn);
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
