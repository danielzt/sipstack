package io.sipstack.transport;

import io.sipstack.netty.codec.sip.ConnectionId;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Flow {

    /**
     * Ok, perhaps this should be called FlowId but it is
     * exactly the same thing so it felt silly...
     *
     * @return
     */
    ConnectionId id();

    default boolean isValid() {
        return true;
    }

    default boolean isFailed() {
        return !isValid();
    }

    default String failedReason() {
        return "bajs";
    }
}
