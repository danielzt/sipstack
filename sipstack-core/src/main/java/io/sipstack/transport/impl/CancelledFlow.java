package io.sipstack.transport.impl;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public class CancelledFlow extends InternalFlow {

    public CancelledFlow() {
        super(Optional.empty());
    }

    public boolean isValid() {
        return false;
    }

}
