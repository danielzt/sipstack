/**
 * 
 */
package io.sipstack.example.application;

import io.sipstack.annotations.INVITE;
import io.sipstack.core.Application;
import io.sipstack.core.Bootstrap;
import io.sipstack.core.Environment;
import io.sipstack.netty.codec.sip.event.SipMessageEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author jonas@jonasborjesson.com
 */
public final class MySipApplication extends Application<MyConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(MySipApplication.class);

    /**
     * 
     */
    public MySipApplication() {
        super("My cool SIP application");
    }

    @INVITE
    public void processInvite(final SipMessageEvent event) {
        logger.info("Recevied a new sip event");
    }

    @Override
    public void initialize(final Bootstrap<MyConfiguration> bootstrap) {
        System.err.println("I'm being bootstrapped");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final MyConfiguration configuration, final Environment environment) throws Exception {
        System.err.println("I'm running");
        environment.addResource(this);
    }

    public static void main(final String ... args) throws Exception {
        new MySipApplication().run(args);
    }

}
