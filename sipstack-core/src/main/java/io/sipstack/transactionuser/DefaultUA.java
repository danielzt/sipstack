package io.sipstack.transactionuser;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.address.URI;
import io.sipstack.application.DefaultApplicationContext;

/**
 * @author ajansson@twilio.com
 */
public class DefaultUA implements UA {

    private final DefaultApplicationContext ctx;
    private final String friendlyName;
    private final URI target;
    private final SipRequest request;

    public DefaultUA(final DefaultApplicationContext ctx, final String friendlyName, final SipRequest request, final URI target) {
        this.ctx = ctx;
        this.friendlyName = friendlyName;
        this.request = request;
        this.target = target;
    }

    public String friendlyName() {
        return friendlyName;
    }

    public SipRequest getRequest() {
        return request;
    }

    public URI getTarget() {
        return target;
    }

    @Override
    public void send(final SipMessage message) {
        ctx.send(message);
    }
}
