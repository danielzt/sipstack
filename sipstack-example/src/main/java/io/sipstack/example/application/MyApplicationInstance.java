package io.sipstack.example.application;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.application.ApplicationInstance;
import io.sipstack.application.SipEvent;
import io.sipstack.application.SipRequestEvent;
import io.sipstack.event.Event;
import io.sipstack.transactionuser.Proxy;
import io.sipstack.transactionuser.ProxyBranch;

/**
 * @author jonas@jonasborjesson.com
 */
public class MyApplicationInstance extends ApplicationInstance {

    public MyApplicationInstance(final Buffer id) {
        super(id);
    }

    @Override
    public void onRequest(final SipRequestEvent request) {

        System.err.println("yeah! My application instance got the request!");
        final SipURI to = SipURI.withUser("hello").withHost("127.0.0.1").withPort(5090).build();
        Proxy.Builder builder = proxy(to);
        builder.onBranchFailure(this::onBranchFailure);
        builder.build().start();


        /*

        UA a;
        a.onByeResponse().filter(r -> r.isSuccessResponse()).thenDo(this::onBranchFailure);
        a.on("invite").doRequest().filter();
        a.on("invite").doResponse().filter();

        a.onByeRequest().filter(response is 200).then(this::onBranchFailure);

        UA a = ctx().withNamedUA("nisse").withTarget("sip:sometwhere@asdf").build();
        UA b = ctx().withNamedUA("nisse").withTarget("sip:sometwhere@asdf").build();

        b.onSuccess(this::handleBSuccessResponse);

        B2bua b2b = ctx().b2bua().withA(a).withB(b).start();

        b2b.onSideBFailure(response -> {
            ctx().replaceBWith(ctx().withNamedUA("b-try2")).goAGain();

        });

        if (request.isInvite()) {
            get("http://whitepages.com/v1/asdf").withHeader("xxxx").onSuccess(result -> {
                if (state == stillGoingString) {
                    UA a = ctx().withNamedUA("nisse").withTarget("sip:sometwhere@asdf").build();
                } else {
                    ops;
                }
            }).onAppState(killed).onFailure(result -> {

            }).fetch();
        } else if (request.isCancel()) {
            killInstance();
            transition(dead);
        }
        */
    }

    public void onBranchFailure(final ProxyBranch branch, final Event event) {
        System.err.println("Branched failed");
    }

}
