package io.sipstack.transport.impl;

import io.sipstack.transport.FlowState;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public class FailureFlow extends InternalFlow {


    private final Throwable cause;

    public FailureFlow(final Throwable cause) {
        super(Optional.empty(), FlowState.CLOSED);
        this.cause = cause;
    }

    public boolean isValid() {
        return false;
    }


}
