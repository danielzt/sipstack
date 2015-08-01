package io.sipstack.transport.impl;

import io.sipstack.transport.FlowFuture;

/**
 * @author jonas@jonasborjesson.com
 */
public class SuccessfulFlowFuture implements FlowFuture {

    @Override
    public boolean cancel() {
        return false;
    }
}
