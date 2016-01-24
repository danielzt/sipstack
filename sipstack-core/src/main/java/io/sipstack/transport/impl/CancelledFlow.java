package io.sipstack.transport.impl;

import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowState;

import java.util.Optional;
import java.util.concurrent.CancellationException;

/**
 * Represents a cancelled {@link Flow} and is really no difference
 * than a {@link FailureFlow} but we are forcing this one to use
 * a {@link CancellationException}
 *
 * @author jonas@jonasborjesson.com
 */
public class CancelledFlow extends FailureFlow {
    public CancelledFlow(final ConnectionId id, final CancellationException cause) {
        super(id, cause);
    }
}
