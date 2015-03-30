/**
 * 
 */
package io.sipstack.cli;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

/**
 * @author jonas@jonasborjesson.com
 */
public class CommandLineArgs {

    private CmdLineParser parser;

    @Argument(metaVar = "<command>", usage = "where command is ")
    private final List<String> arguments = new ArrayList<>();

    @Option(name = "-h", aliases = "-help", usage = "print this message")
    private boolean help;

    @Option(name = "-c", aliases = { "-getConfig"}, metaVar = "<getConfig>",
            usage = "use given configuration file")
    private File configFile;

    /**
     * can only construct it via the {@link #parse(String...) method.
     */
    private CommandLineArgs() {
        // left empty intentionally
    }

    public boolean isHelp() {
        return this.help;
    }

    private void setParser(final CmdLineParser parser) {
        this.parser = parser;
    }

    public File getConfigFile() {
        return this.configFile;
    }

    public void printUsage(final OutputStream out) {
        this.parser.printUsage(out);
    }

    public void printUsage() {
        printUsage(System.err);
    }

    /**
     * Parse the arguments into a {@link CommandLineArgs}.
     * 
     * @param arguments the arguments to parse
     * @return a new {@link CommandLineArgs} or null if either the user asked for help.
     * @throws CmdLineException in case anything goes wrong while parsing the arguments.
     */
    public static CommandLineArgs parse(final String...arguments) throws CmdLineException {
        final ParserProperties props = ParserProperties.defaults();
        props.withUsageWidth(80);

        final CommandLineArgs args = new CommandLineArgs();
        final CmdLineParser parser = new CmdLineParser(args, props);
        args.setParser(parser);
        parser.parseArgument(arguments);
        return args;
    }


}
