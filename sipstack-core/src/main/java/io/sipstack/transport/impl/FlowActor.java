package io.sipstack.transport.impl;

import io.sipstack.actor.Actor;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowId;

/**
 * The {@link FlowActor} is realizing a {@link Flow} and
 * implements the following state machine:
 *
 *
 *
 * <pre>
 *     Currently displayed here while I figure out exactly
 *     how the FSM should look like:
 *     http://www.gliffy.com/go/publish/image/9427857/L.png
 * </pre>
 *
 * States:
 * <ul>
 *     <li>INIT - the state machine was just created and </li>
 *     <li></li>
 *     <li></li>
 *     <li></li>
 *     <li></li>
 * </ul>
 *
 * @author jonas@jonasborjesson.com
 */
public interface FlowActor extends Actor {

    /**
     * Get the {@link Flow} that represents this actor. The {@link Flow} is simply
     * a snapshot of what the {@link FlowActor} looked like at that particular time
     * and may have changed. However, the {@link Flow} can always be used to interact
     * with the underlying actor, which is of course hidden to the user and only
     * an implementation detail.
     *
     * @return
     */
    Flow flow();

    /**
     * Conveience method for obtaining the {@link FlowId}, which is the unique key
     * for this flow.
     * @return
     */
    FlowId id();

    Connection connection();
}
