package io.sipstack.netty.codec.sip.actor;

/**
 * Very specialized actor context fitting the Netty pipeline model
 * which is why you don't talk to other actors but just fires event
 * upstream and downstream. Hence, this entire so-called actor framework
 * is more of an abstraction of FSM + Netty kind of...
 *
 * @author jonas@jonasborjesson.com
 */
public interface ActorContext {

}
