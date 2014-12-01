/**
 * 
 */
package io.sipstack.server;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;
import io.sipstack.config.Configuration;
import io.sipstack.core.Environment;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.annotation.Resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jonas@jonasborjesson.com
 */
public class Server<T extends Configuration> {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    /**
     * 
     */
    private Server() {
        // TODO Auto-generated constructor stub
    }

    public static Builder with(final Environment environment) throws IllegalArgumentException {
        return new Builder(ensureNotNull(environment));
    }

    public static class Builder {

        private static final Pattern WINDOWS_NEWLINE = Pattern.compile("\\r\\n?");

        private final Environment environment;

        private Builder(final Environment environment) {
            this.environment = environment;
        }

        /**
         * Print banner. Copied from Dropwizard.io.
         * 
         * @param name
         */
        protected void printBanner(final String name) {
            try {
                final String banner = WINDOWS_NEWLINE.matcher(Resources.toString(Resources.getResource("banner.txt"),
                        Charsets.UTF_8))
                        .replaceAll("\n")
                        .replace("\n", String.format("%n"));
                logger.info(String.format("Starting {}%n{}"), name, banner);
            } catch (IllegalArgumentException | IOException ignored) {
                // don't display the banner if there isn't one
                logger.info("Starting {}", name);
            }
        }

    }

}
