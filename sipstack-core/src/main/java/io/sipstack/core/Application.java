/**
 * 
 */
package io.sipstack.core;

import static io.pkts.packet.sip.impl.PreConditions.checkIfEmpty;
import io.sipstack.cli.CommandLineArgs;
import io.sipstack.config.Configuration;
import io.sipstack.utils.Generics;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * @author jonas@jonasborjesson.com
 */
public abstract class Application<T extends Configuration> {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private final String name;

    /**
     * Default empty constructor.
     */
    public Application(final String name) {
        this.name = checkIfEmpty(name) ? "Default SIP Application" : name;
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
        return mapper.readValue(stream, (Class<T>)Generics.getTypeParameter(getClass()));
    }

}
