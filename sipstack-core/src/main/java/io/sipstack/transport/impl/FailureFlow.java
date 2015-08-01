package io.sipstack.transport.impl;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public class FailureFlow extends InternalFlow {


    private final Throwable cause;

    public FailureFlow(final Throwable cause) {
        super(Optional.empty());
        this.cause = cause;
    }

    public boolean isValid() {
        return false;
    }


}
