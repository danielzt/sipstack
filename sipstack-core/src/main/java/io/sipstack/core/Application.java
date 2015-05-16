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
import io.hektor.config.HektorConfiguration;
import io.hektor.core.ActorRef;
import io.hektor.core.Hektor;
import io.hektor.core.Props;
import io.hektor.core.RoutingLogic;
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.cli.CommandLineArgs;
import io.sipstack.config.Configuration;
import io.sipstack.config.NetworkInterfaceConfiguration;
import io.sipstack.config.NetworkInterfaceDeserializer;
import io.sipstack.config.SipConfiguration;
import io.sipstack.net.NetworkLayer;
import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.netty.codec.sip.SipMessageEvent;
import io.sipstack.server.SipBridgeHandler;
import io.sipstack.transport.TransportSupervisor;
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
     * Initializes the application bootstrap.
     *
     * @param bootstrap the application bootstrap
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
     * Parses command-line arguments and runs the application. Call this method from a {@code public
     * static void main} entry point in your application.
     *
     * @param arguments the command-line arguments
     * @throws Exception if anything goes wrong. The application will halt.
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
            final NetworkLayer.Builder networkBuilder = NetworkLayer.with(ifs);

            // configure Hektor
            final HektorConfiguration hektorConfig = config.getHektorConfiguration();
            final Hektor hektor = Hektor.withName("hello").withConfiguration(hektorConfig).build();
            final ActorRef transportSupervisorRef = configureActorSystem(hektor);

            final SipBridgeHandler handler = new SipBridgeHandler(transportSupervisorRef);
            networkBuilder.serverHandler(handler);

            final NetworkLayer server = networkBuilder.build();
            server.start();

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
        // 3a. Call application.run(configuration, environment);
        // 3a. call run(configuration, environment) on itself.

        // Bootstrap.run will:
        // 1. For each Bundle call bundle.run(environment)
        //        AssetBundle
        //        HelloWorldApplication$2 - guess this must be a bundlefied version of my application
        // 2. For each ConfigureBundle call bundle.run(environment)
        //        HelloWorldApplication$1 - guess this must be a bundlefied version of my application

        // ServerCommand.run will:
        // Create a new Jetty Server and will then start it, which is what starts the Jetty Server.

    }

    private ActorRef configureActorSystem(final Hektor hektor) throws NoSuchMethodException {

        // The transport supervisor is responsible for maintaining flows etc.
        // Hence it is responsible for keeping track of connections and everything
        // regarding the "last mile".
        Hektor.RouterBuilder routerBuilder = hektor.routerWithName("transport-router");
        routerBuilder.withRoutingLogic(new SipMessageRoutingLogic());
        for (int i = 0; i < 4; ++i) {
            final Props props = Props.forActor(TransportSupervisor.class).build();
            final ActorRef ref = hektor.actorOf(props, "transport-" + i);
            routerBuilder.withRoutee(ref);
        }

        return routerBuilder.build();
    }

    private static class SipMessageRoutingLogic implements RoutingLogic {

        @Override
        public ActorRef select(final Object msg, final List<ActorRef> routees) {
            final SipMessageEvent sipMsgEvent = (SipMessageEvent)msg;
            final ConnectionId id = sipMsgEvent.getConnection().id();
            // System.err.println("Selecting routee based on " + id);
            return routees.get(Math.abs(id.hashCode() % routees.size()));
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
