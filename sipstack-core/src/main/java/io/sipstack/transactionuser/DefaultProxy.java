package io.sipstack.transactionuser;

import io.pkts.packet.sip.SipRequest;

import java.util.List;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultProxy implements Proxy {

    private final String friendlyName;
    private boolean started;
    private boolean actuallyStarted;

    /**
     * TODO: we need to have another class that holds all proxy branches
     * sorted by priority & weight etc. For now, let's just keep it
     * simple and get stuff working...
     */
    private List<DefaultProxyBranch> branches;

    /**
     * The {@link SipRequest} that is being proxied.
     */
    private final SipRequest request;

    public DefaultProxy(final String friendlyName, final SipRequest request, final List<DefaultProxyBranch> branches) {
        this.friendlyName = friendlyName;
        this.request = request;
        this.branches = branches;
    }

    public String friendlyName() {
        return friendlyName;
    }

    /**
     * If the user has requested the proxy to be started but
     * we are yet to actually start it then this proxy needs
     * to be started.
     *
     * @return
     */
    public boolean needsToBeStarted() {
        return started && !actuallyStarted;
    }

    public void actuallyStart() {
        if (!needsToBeStarted()) {
            return;
        }
        actuallyStarted = true;
        branches.forEach(DefaultProxyBranch::start);
    }

    @Override
    public SipRequest request() {
        return request;
    }

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void cancel() {

    }

    @Override
    public void terminate() {

    }

}
