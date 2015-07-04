package io.sipstack.netty.codec.sip.tu;

import io.sipstack.netty.codec.sip.event.Event;

import java.util.function.Consumer;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ProxyBranch {

    /**
     * Obtain the {@link Proxy} to which this branch belongs.
     *
     * @return
     */
    // Proxy proxy();

    /**
     * Try to cancel this branch.
     *
     * @return true if this branch was in a state so that we could
     * cancel it, false otherwise.
     */
    boolean cancel();

    interface Builder {

        Builder withPriority(int priority);

        Builder withWeight(int weight);

        Builder onFailure(Consumer<Event> consumer);

    }

}
