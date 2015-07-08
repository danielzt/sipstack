package io.sipstack.transport;

import io.sipstack.netty.codec.sip.ConnectionId;
import io.sipstack.netty.codec.sip.Transport;

import java.util.function.Consumer;

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

    interface Builder {

        Builder withPort(int port);

        Builder withTransport(Transport transport);

        Builder onSuccess(Consumer<Flow> consumer);

        Builder onFailure(Consumer<Flow> consumer);

        Builder onCancelled(Consumer<Flow> consumer);

        /**
         *
         * @return
         * @throws IllegalArgumentException in case any of the supplied arguments are wrong
         * or some of the mandatory arguments are missing.
         */
        FlowFuture connect() throws IllegalArgumentException;
    }
}
