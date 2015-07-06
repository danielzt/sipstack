package io.sipstack.example.application;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipRequest;
import io.sipstack.netty.codec.sip.application.ApplicationInstance;
import io.sipstack.netty.codec.sip.event.Event;
import io.sipstack.netty.codec.sip.tu.ProxyBranch;

/**
 * @author jonas@jonasborjesson.com
 */
public class MyApplicationInstance extends ApplicationInstance {

    public MyApplicationInstance(final Buffer id) {
        super(id);
    }

    public void onRequest(final SipRequest request) {
        System.err.println("yeah! My application instance got the request!");

        /*
        final SipURI to = SipURI.withUser("hello").withHost("127.0.0.1").withPort(5090).build();
        Proxy.Builder builder = proxy(to);
        builder.onBranchFailure(this::onBranchFailure);
        builder.build().start();
        */
    }

    public void onBranchFailure(final ProxyBranch branch, final Event event) {
        System.err.println("Branched failed");
    }

}
