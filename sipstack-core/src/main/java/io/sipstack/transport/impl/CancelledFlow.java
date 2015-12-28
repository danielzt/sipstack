package io.sipstack.transport.impl;

import io.sipstack.transport.FlowState;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public class CancelledFlow extends InternalFlow {

    public CancelledFlow() {
        super(Optional.empty(), FlowState.CLOSED);
    }

    public boolean isValid() {
        return false;
    }

}
