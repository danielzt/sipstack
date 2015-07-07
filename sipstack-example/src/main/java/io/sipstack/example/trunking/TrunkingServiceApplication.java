/**
 * 
 */
package io.sipstack.example.trunking;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.header.SipHeader;
import io.sipstack.application.ApplicationInstance;
import io.sipstack.application.ApplicationInstanceCreator;
import io.sipstack.core.Application;
import io.sipstack.core.Bootstrap;
import io.sipstack.core.Environment;
import io.sipstack.example.application.MyApplicationInstance;
import io.sipstack.example.application.MyConfiguration;


/**
 * @author jonas@jonasborjesson.com
 */
public final class TrunkingServiceApplication extends Application<MyConfiguration> {

    /**
     *
     */
    public TrunkingServiceApplication() {
        super("Trunking Service");
    }

    @Override
    public void initialize(final Bootstrap<MyConfiguration> bootstrap) {
        //bootstrap.getMetricRegistry();
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
        return new MyInstanceCreator();
    }

    private static class MyInstanceCreator implements ApplicationInstanceCreator {

        @Override
        public Buffer getId(final SipMessage message) {
            return message.getCallIDHeader().getCallId();
        }

        @Override
        public ApplicationInstance createInstance(final Buffer id, final SipMessage message) {
            if (message.isInitial() && message.isInvite()) {
                return new TrunkingServiceApplicationInstance(id, message);
            } else {
                return new ApplicationInstance(id) {
                };
            }
        }

    }

    public static void main(final String ... args) throws Exception {
        new TrunkingServiceApplication().run(args);
    }

}
