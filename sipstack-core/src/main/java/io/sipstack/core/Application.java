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
import io.pkts.packet.sip.impl.PreConditions;
import io.sipstack.actor.ActorRef;
import io.sipstack.actor.ActorSystem;
import io.sipstack.actor.WorkerContext;
import io.sipstack.application.ApplicationSupervisor;
import io.sipstack.cli.CommandLineArgs;
import io.sipstack.config.Configuration;
import io.sipstack.config.NetworkInterfaceConfiguration;
import io.sipstack.config.NetworkInterfaceDeserializer;
import io.sipstack.config.SipConfiguration;
import io.sipstack.net.NetworkLayer;
import io.sipstack.server.SipBridgeHandler;
import io.sipstack.transaction.impl.TransactionSupervisor;
import io.sipstack.transport.TransportSupervisor;
import io.sipstack.utils.Generics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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

            final ActorSystem system = configureActorSystem(config);
            final SipBridgeHandler handler = new SipBridgeHandler(system);
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
        // 3a. Call bootsrap.run(configuration, environment)
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

    private ActorSystem configureActorSystem(final Configuration config) {
        final SipConfiguration sipConfig = config.getSipConfiguration();
        final int noOfWorkers = sipConfig.getWorkerThreads();

        final ActorRef systemRef = ActorRef.withWorkerPool(-1).withName("/").withNoOfWorkers(noOfWorkers).build();
        final ActorSystem.Builder builder = ActorSystem.withName("sipstack.io");
        builder.withActorRef(systemRef);

        for (int i = 0; i < noOfWorkers; ++i) {

            // TODO: may want this to be configurable as well.
            final ActorRef refTransport = ActorRef
                    .withWorkerPool(i)
                    .withParent(systemRef)
                    .withName("transportSupervisor")
                    .withNoOfWorkers(noOfWorkers)
                    .build();
            final TransportSupervisor transportSupervisor = new TransportSupervisor(refTransport);

            final ActorRef refTransaction = ActorRef
                    .withWorkerPool(i)
                    .withParent(systemRef)
                    .withName("transactionSupervisor")
                    .withNoOfWorkers(noOfWorkers)
                    .build();
            final TransactionSupervisor transactionSupervisor = new TransactionSupervisor(sipConfig.getTransaction());

            final ActorRef refApplication = ActorRef
                    .withWorkerPool(i)
                    .withParent(systemRef)
                    .withName("applicationSupervisor")
                    .withNoOfWorkers(noOfWorkers)
                    .build();
            final ApplicationSupervisor applicationSupervisor = new ApplicationSupervisor(refApplication, getApplicationMapper());

            // TODO: should probably be configurable as well
            final BlockingQueue<Runnable> jobQueue = new LinkedBlockingQueue<Runnable>(100);

            final WorkerContext.Builder wcBuilder =
                    WorkerContext.withDefaultChain(transportSupervisor, transactionSupervisor, applicationSupervisor);
            wcBuilder.withQueue(jobQueue);
            builder.withWorkerContext(wcBuilder.build());
        }

        return builder.build();
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
