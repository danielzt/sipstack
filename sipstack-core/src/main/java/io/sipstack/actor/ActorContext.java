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
public interface ActorContext {

    /**
     * Like a ChannelHandler in Netty, an "actor" in this sip codec project
     * always knows which direction events should be sent. E.g. a
     * {@link InviteServerTransaction} will always forward requests
     * upstream and responses downstream.
     *
     * @param event
     */
    void forwardUpstream(Event event);

    void forwardDownstream(Event event);

    Scheduler scheduler();

}
