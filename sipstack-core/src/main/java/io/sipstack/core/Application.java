/**
 * 
 */
package io.sipstack.core;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import io.hektor.core.ActorRef;
import io.hektor.core.Hektor;
import io.hektor.core.RoutingLogic;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.actor.HashWheelScheduler;
import io.sipstack.actor.InternalScheduler;
import io.sipstack.application.ApplicationController;
import io.sipstack.application.ApplicationInstanceCreator;
import io.sipstack.cli.CommandLineArgs;
import io.sipstack.config.Configuration;
import io.sipstack.config.NetworkInterfaceConfiguration;
import io.sipstack.config.NetworkInterfaceDeserializer;
import io.sipstack.config.SipConfiguration;
import io.sipstack.event.Event;
import io.sipstack.net.NettyNetworkLayer;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.netty.codec.sip.event.impl.SipMessageIOEventImpl;
import io.sipstack.netty.codec.sip.SystemClock;
import io.sipstack.transaction.impl.DefaultTransactionLayer;
import io.sipstack.transport.impl.DefaultTransportLayer;
import io.sipstack.utils.Generics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author jonas@jonasborjesson.com
 */
public abstract class Application<T extends Configuration> {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private final String name;

    private static final ApplicationMapper DEFAULT_APPLICATION_MAPPER = new ApplicationMapper(){};

    /**
     * Default empty constructor.
     */
    public Application(final String name) {
        this.name = PreConditions.checkIfEmpty(name) ? "Default SIP Application" : name;
    }

    public String getName() {
        return this.name;
    }


    /**
     * Initializes the io.sipstack.application.application bootstrap.
     *
     * @param bootstrap the io.sipstack.application.application bootstrap
     */
    public void initialize(final Bootstrap<T> bootstrap) {
        // sub-classes should override
    }

    public ApplicationMapper getApplicationMapper() {
        return DEFAULT_APPLICATION_MAPPER;
    }

    /**
     * Will be called when the application runs. Override it to add
     * resources etc for your application.
     * 
     * Note that any exception that escapes this method will cause 
     * the application to halt. Typically this is what you want since
     * you do not ever want to run your application in a
     * half-initialized/unknown stage. Hence, make sure that you do
     * handle all the exceptions that you can indeed handle but if
     * you do not have a sane default behavior for dealing with a 
     * particular exception then you really should let it escape so 
     * that the server shuts down.
     * 
     * @param configuration the parsed {@link Configuration} object
     * @throws Exception if anything goes wrong. The application will halt.
     */
    public abstract void run(T configuration, Environment environment) throws Exception;

    /**
     *
     * @return
     */
    public abstract ApplicationInstanceCreator applicationCreator();

    /**
     * Parses command-line arguments and runs the io.sipstack.application.application. Call this method from a {@code public
     * static void main} entry point in your io.sipstack.application.application.
     *
     * @param arguments the command-line arguments
     * @throws Exception if anything goes wrong. The io.sipstack.application.application will halt.
     */
    public final void run(final String... arguments) throws Exception {

        try {
            final CommandLineArgs args = CommandLineArgs.parse(arguments);
            if (args.isHelp()) {
                args.printUsage(System.err);
                return;
            }

            final T config = loadConfiguration(getConfigurationFile(args));

            // Create bootstrap
            final Bootstrap<T> bootstrap = new Bootstrap<>(this);

            // initialize
            initialize(bootstrap);

            // build new environment
            final Environment.Builder builder = Environment.withName(this.getName());
            builder.withMetricRegistry(bootstrap.getMetricRegistry());
            final Environment environment = builder.build();

            // start the application
            run(config, environment);

            // Create and initialize the actual Sip server
            final SipConfiguration sipConfig = config.getSipConfiguration();
            final List<NetworkInterfaceConfiguration> ifs = sipConfig.getNetworkInterfaces();
            final NettyNetworkLayer.Builder networkBuilder = NettyNetworkLayer.with(ifs);

            final EventLoopGroup boosGroup = new NioEventLoopGroup();
            final EventLoopGroup udpTcpGroup = new NioEventLoopGroup();
            networkBuilder.withBossEventLoopGroup(boosGroup);
            networkBuilder.withUDPEventLoopGroup(udpTcpGroup);
            networkBuilder.withTCPEventLoopGroup(udpTcpGroup);

            // The internal scheduler is used to schedule
            // internal which primarily are  as SIP timers.
            // final InternalScheduler scheduler = new DefaultInternalScheduler(udpTcpGroup);
            final InternalScheduler scheduler = new HashWheelScheduler();
            final Clock clock = new SystemClock();

            final ApplicationController controller = new ApplicationController(clock, scheduler, applicationCreator());

            // Transport layer is responsible for managing connections,
            // i.e. Flows.
            final DefaultTransportLayer transportLayer = new DefaultTransportLayer(sipConfig.getTransport(), clock, scheduler);
            networkBuilder.withHandler("transport-layer", transportLayer);

            // The transaction layer is responsible for transaction
            // management and is typically always present in a
            // SIP stack.
            final DefaultTransactionLayer transactionLayer = new DefaultTransactionLayer(transportLayer, clock, scheduler,sipConfig.getTransaction());
            networkBuilder.withHandler("transaction-layer", transactionLayer);

            // DefaultTransactionUserLayer transactionUserLayer = new DefaultTransactionUserLayer(consumer);

            // final SipStack stack = SipStack.withConfiguration(sipConfig)
                    // .withClock(clock)
                    // .withScheduler(scheduler)
                    // .withConsumer(controller)
                    // .build();
            // networkBuilder.withHandler(stack.handler());

            final NettyNetworkLayer server = networkBuilder.build();

            // catch 22. The transport layers needs a reference to the network layer
            // and the network layer needs transport layer to be part of the
            // netty chain.
            transportLayer.useNetworkLayer(server);

            server.start();

            // controller.start(stack.getTransactionUserLayer());

            // will wait until server shuts down again.
            server.sync();

        } catch (JsonParseException | JsonMappingException e) {
            logger.error("Unable to parse the configuration file", e);
            throw e;
        } catch (final FileNotFoundException e) {
            logger.error(e.getMessage());
            throw e;
        } catch (final IOException e) {
            logger.error("Problem reading from the configuration file");
            throw e;
        }

        // parse command line. By default the 'server' command
        // that comes with DropWizard kick starts everything.
        // not sure we really need it. Is there a stop perhaps?
        // But how is that one connected to when the JVM is
        // already up and running?

        // The server command does:
        // 1. Parse the configuration file
        // 2. Calls run on itself
        // 3. Runs a EnvironmentCommand that will:
        // 3a. Create a new Environment
        // 3a. Call ootsrap.run(configuration, environment)
        // 3a. Call io.sipstack.application.application.run(configuration, environment);
        // 3a. call run(configuration, environment) on itself.

        // Bootstrap.run will:
        // 1. For each Bundle call bundle.run(environment)
        //        AssetBundle
        //        HelloWorldApplication$2 - guess this must be a bundlefied version of my io.sipstack.application.application
        // 2. For each ConfigureBundle call bundle.run(environment)
        //        HelloWorldApplication$1 - guess this must be a bundlefied version of my io.sipstack.application.application

        // ServerCommand.run will:
        // Create a new Jetty Server and will then start it, which is what starts the Jetty Server.

    }

    /**
     * Builder to setup the actor chains for everything needed by the container.
     * Primarily, this is to setup the SIP related chains, going from the
     * transport layer, to io.sipstack.transaction.transaction, to dialog, to io.sipstack.application.application etc.
     *
     */
    public static class ActorSystemBuilder {

        private final Configuration config;
        private Hektor hektor;

        public static ActorSystemBuilder withConfig(final Configuration config) {
            return new ActorSystemBuilder(config);
        }

        private ActorSystemBuilder(final Configuration config) {
            this.config = config;
        }

        public ActorSystemBuilder withHektor(final Hektor hektor) {
            this.hektor = hektor;
            return this;
        }

        public ActorRef build() {
            final int noOfRoutees = 4;

            // The transport supervisor is responsible for maintaining flows etc.
            // Hence it is responsible for keeping track of connections and everything
            // regarding the "last mile".
            Hektor.RouterBuilder routerBuilder = hektor.routerWithName("transport-router");
            routerBuilder.withRoutingLogic(new ConnectionRoutingLogic());


            Hektor.RouterBuilder transactionRouterBuilder = hektor.routerWithName("io.sipstack.transaction.transaction-router");
            transactionRouterBuilder.withRoutingLogic(new SipMessageRoutingLogic());

            // TODO: the routing logic for the io.sipstack.application.application router should be configurable
            // by the user. Default will be on dialog or something. Currently it will be on
            // Call-ID.
            Hektor.RouterBuilder applicationRouterBuilder = hektor.routerWithName("io.sipstack.application.application-router");
            applicationRouterBuilder.withRoutingLogic(new SipMessageRoutingLogic());

            // this is kind of stupid but the various supervisors
            // need to know about their "upstream" and "downstream"
            // partner so that new requests can correctly be established
            // through the chain which requires to go through each supervisor.
            // However, since they are all dependent on each other, we cannot
            // pass it in through the constructor so will have to do it using a
            // init-message.
            // final ActorRef[] transportActors = new ActorRef[noOfRoutees];
            // final ActorRef[] transactionActors = new ActorRef[noOfRoutees];
            // final ActorRef[] applicationActors = new ActorRef[noOfRoutees];


            /*
            for (int i = 0; i < noOfRoutees; ++i) {
                final Props props = Props.forActor(TransportSupervisor.class)
                        .withConstructorArg(config.getSipConfiguration().getTransport())
                        .build();
                final ActorRef transportActor = hektor.actorOf(props, "transport-" + i);
                transportActors[i] = transportActor;
                routerBuilder.withRoutee(transportActor);


                final TransactionLayerConfiguration transactionConfig = config.getSipConfiguration().getTransaction();
                final Props transactionProps = Props.forActor(TransactionSupervisor.class)
                        .withConstructorArg(transactionConfig)
                        .build();
                final ActorRef transactionActor = hektor.actorOf(transactionProps, "io.sipstack.transaction.transaction-" + i);
                transactionActors[i] = transactionActor;
                transactionRouterBuilder.withRoutee(transactionActor);

                final Props appProps = Props.forActor(ApplicationSupervisor.class).build();
                final ActorRef applicationActor = hektor.actorOf(appProps, "io.sipstack.application.application-" + i);
                applicationActors[i] = applicationActor;
                applicationRouterBuilder.withRoutee(applicationActor);
            }
            */

            // final ActorRef transportRouter = routerBuilder.build();
            // final ActorRef transactionRouter = transactionRouterBuilder.build();
            // final ActorRef applicationRouter = applicationRouterBuilder.build();

            // Transport Supervisors do not have a downstream element
            // but the upstream is the io.sipstack.transaction.transaction router
            // final InitEvent initTransportActors = new InitEvent(null, transactionRouter);

            // Transaction Supervisors are connected to the the transport supervisor
            // in the downstream direction and the io.sipstack.application.application router in the upstream
            // direction.
            // final InitEvent initTransactionActors = new InitEvent(transportRouter, applicationRouter);

            // TODO: Want to insert dialog support here as well as the TU layer

            // Application Supervisors are at the end of the line so they do not have
            // an upstream element but in the opposite direction they will talk
            // to the io.sipstack.transaction.transaction supervisors
            // final InitEvent initApplicationActors = new InitEvent(transactionRouter, null);


            // for (int i = 0; i < noOfRoutees; ++i) {
                // transportActors[i].tellAnonymously(initTransportActors);
                // transactionActors[i].tellAnonymously(initTransactionActors);
                // applicationActors[i].tellAnonymously(initApplicationActors);
            // }

            // TODO: need to hook up stuff to the TransactionSupervisor
            // via a router. However, they both need to know about each
            // other, which is kind of annoying.

            // return transportRouter;
            return null;
        }
    }

    private static class ConnectionRoutingLogic implements RoutingLogic {

        @Override
        public ActorRef select(final Object msg, final List<ActorRef> routees) {
            final SipMessageIOEventImpl sipMsgEvent = (SipMessageIOEventImpl)msg;
            final ConnectionId id = sipMsgEvent.connection().id();
            return routees.get(Math.abs(id.hashCode() % routees.size()));
        }
    }

    /**
     * Basic routing logic using the SIP Call-ID as the base for the actor
     * selection.
     */
    private static class SipMessageRoutingLogic implements RoutingLogic {

        @Override
        public ActorRef select(final Object msg, final List<ActorRef> routees) {
            final Event event = (Event)msg;
            /*
            if (event.isSipIOEvent()) {
                final SipMessage sip = (SipMessage)event.toIOEvent().getObject();
                final Buffer callId = sip.getCallIDHeader().getCallId();
                return routees.get(Math.abs(callId.hashCode() % routees.size()));
            }
            */
            throw new RuntimeException("[SipMessageRoutingLogic] It wasn't a SIP event so not sure what to do.");
        }
    }

    /**
     * If the configuration file is given on the command line then it will be used. If none is given
     * then we will try and locate a default one.
     * 
     * @param args
     * @return
     * @throws FileNotFoundException 
     */
    private InputStream getConfigurationFile(final CommandLineArgs args) throws FileNotFoundException {
        // TODO: actually do what the javadoc says.
        if (args.getConfigFile() == null) {
            throw new FileNotFoundException("No configuration file specified");
        }
        return new FileInputStream(args.getConfigFile());
    }

    @SuppressWarnings("unchecked")
    public <T> T loadConfiguration(final InputStream stream) throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(NetworkInterfaceConfiguration.class, new NetworkInterfaceDeserializer());
        mapper.registerModule(module);
        mapper.registerModule(new JSR310Module()); 
        return mapper.readValue(stream, (Class<T>)Generics.getTypeParameter(getClass()));
    }

}
