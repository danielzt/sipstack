/**
 * 
 */
package io.sipstack.example.application;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.application.ApplicationInstance;
import io.sipstack.application.ApplicationInstanceCreator;
import io.sipstack.core.Application;
import io.sipstack.core.Bootstrap;
import io.sipstack.core.Environment;
import io.sipstack.netty.codec.sip.SipMessageEvent;
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
        super("My cool SIP io.sipstack.application.application");
    }

    public void processInvite(final SipMessageEvent event) {
        logger.info("Recevied a new sip event");

        // ProxyBranch branch = ProxyBranch.withTarget("sip:hello.com").withRoute().withRoute().withHeader(header).build();
        // ctx().proxyRequest(event).withXXX().withYYY().withBranch(branch).start();
        // ctx().doProxy(event).withXXX().onBranchFailure(b ->);
        // proxy().cancel(); // does it nicely and let's the app know whats going on
        // proxy().terminate(); // does it nicely but doesn't let the app know.
        // proxy().die(); // just fucking kills it. Cleans out all memory. Its gone.

        // ctx().invite("sip:xxxx.om"); // will create a new UA instance builder.
        // ctx().subscribe("sip:xxxx.om"); // will create a new UA instance builder.
        // ctx().method(); // new UA instance.
        // ctx().get("http://whatever.com").accept("io.sipstack.application.application/json").onFailure(f -> do something);

        // ua().invite("sip:xxxxx.com").withCSeq().withVia().send();
        // ua(); // will return the current UA instance associated with the current event.
        // ua().onRetransmit();
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

    @Override
    public ApplicationInstanceCreator applicationCreator() {
        return new DefaultApplicationInstanceCreator();
    }

    private static class DefaultApplicationInstanceCreator implements ApplicationInstanceCreator {

        @Override
        public Buffer getId(final SipMessage message) {
            return message.getCallIDHeader().getCallId();
        }

        @Override
        public ApplicationInstance createInstance(final Buffer id, final SipMessage message) {
            return new MyApplicationInstance(id);
        }

    }

    public static void main(final String ... args) throws Exception {
        new MySipApplication().run(args);
    }

}
