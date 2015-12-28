/**
 * 
 */
package io.sipstack.netty.codec.sip;


/**
 * Enum for various SIP timers, some of which are defined across several
 * RFCs, others, such as Trying & Timeout, are more generic ones used
 * across the stack.
 *
 * @author jonas@jonasborjesson.com
 */
public enum SipTimer {
    T1, T2, T3, T4, A, B, C, D, E, F, G, H, I, J, K, L, M, Trying, Timeout, Timeout1, Timeout2, Timeout3, Timeout4;
}
