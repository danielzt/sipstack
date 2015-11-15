package io.sipstack.actor;


import io.sipstack.event.Event;

/**
 * Very specialized actor context fitting the Netty pipeline model
 * which is why you don't talk to other actors but just fires event
 * upstream and downstream. Hence, this entire so-called actor framework
 * is more of an abstraction of FSM + Netty kind of...
 *
 * @author jonas@jonasborjesson.com
 */
public interface ActorContext<T> {

    /**
     * Like a ChannelHandler in Netty, an "actor" in this sip codec project
     * always knows which direction events should be sent. E.g. a
     * {@link InviteServerTransaction} will always forward requests
     * upstream and responses downstream.
     *
     * @param event
     */
    void forwardUpstream(T event);

    void forwardDownstream(T event);

    /**
     * Forward the event in the same direction as the event came in.
     * Use this method if your actor doesn't care about direction
     * (or knows the direction) and you just want to continue
     * the chain.
     *
     * @param event
     */
    void forward(T event);

    Scheduler scheduler();

}
