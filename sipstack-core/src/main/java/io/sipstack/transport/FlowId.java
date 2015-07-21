package io.sipstack.transport;

/**
 * The only difference between a {@link FlowId} and a {@link io.sipstack.netty.codec.sip.ConnectionId} is that the
 * flow id is encrypted in order to prevent tampering. Imagine the following scenario:
 *
 * An attacker is maliciously changing the connection id on the Route header to point
 * to localhost:5060. When the stack is about to send out the message it will check the Route header
 * and determine that there is a connection id encoded on the URL and it will therefore use that
 * information to lookup a connection and send the message there, potentially creating a loop
 * in the stack.
 *
 * So, by encrypting the connection id (which now is called a flow id) by a key only known
 * to the server itself then it can detect whether or not someone has been trying to
 * tamper with it and if so, reject the request in some fashion (silently drop, generate a 403
 * or whatever)
 *
 * @author jonas@jonasborjesson.com
 */
public interface FlowId {

}
