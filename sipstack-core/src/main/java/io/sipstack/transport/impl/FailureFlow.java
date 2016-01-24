package io.sipstack.transport.impl;

import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.transport.FlowState;

import java.util.Optional;
import java.util.concurrent.CancellationException;

/**
 * @author jonas@jonasborjesson.com
 */
public class FailureFlow extends InternalFlow {


    private final Throwable cause;
    private final boolean isCancelled;

    public FailureFlow(final ConnectionId id, final Throwable cause) {
        super(id, Optional.empty(), FlowState.CLOSED);
        this.cause = cause;
        this.isCancelled = false;
    }

    public FailureFlow(final ConnectionId id, final CancellationException cause) {
        super(id, Optional.empty(), FlowState.CLOSED);
        this.cause = cause;
        this.isCancelled = true;
    }

    @Override
    public Optional<Throwable> getFailureCause() {
        return Optional.of(cause);
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    public boolean isValid() {
        return false;
    }


}
