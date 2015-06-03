package io.sipstack.netty.codec.sip.actor;

import io.sipstack.netty.codec.sip.SipTimer;

import java.time.Duration;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Scheduler {

    /**
     * Schedule the message to be sent to the receiving actor after a certain delay.
     *
     * @param msg the message to send over to the receiver.
     * @param receiver the receiving actor.
     * @param sender the sending actor.
     * @param delay the delay before sending off the msg.
     */
    // Cancellable schedule(Object msg, Runnable execute, Duration delay);

    /**
     * Schedule a SIP Timer which will be "delivered" back to the Actor that scheduled
     * it when it fires.
     *
     * @param timer
     * @param delay
     * @return
     */
    Cancellable schedule(SipTimer timer, Duration delay);
}
