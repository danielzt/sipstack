package io.sipstack.transactionuser;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.address.SipURI;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultProxyBranch implements ProxyBranch {

    private final SipRequest request;

    private final Buffer branchId;

    private final SipURI target;

    private final int priority;

    private final int weight;

    public DefaultProxyBranch(final Buffer branchId, final SipURI target,
                              final SipRequest request, final int priority, final int weight) {
        this.target = target;
        this.branchId = branchId;
        this.request = request;
        this.priority = priority;
        this.weight = weight;
    }

    public void start() {
        System.out.println("Ok, starting to proxy to target: " + target);

    }

    @Override
    public boolean cancel() {
        return false;
    }

}
