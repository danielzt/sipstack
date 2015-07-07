package io.sipstack.actor;

import io.sipstack.core.SipTimerListener;
import io.sipstack.event.SipTimerEvent;

import java.time.Duration;

/**
 * @author jonas@jonasborjesson.com
 */
public interface InternalScheduler {

    // Cancellable schedule(Runnable job, Duration delay);

    /**
     * Schedule to deliver the particular event to the context after a certain delay.
     *
     * @param ctx the context to which the event will be delivered.
     * @param event the event to deliver
     * @param delay the delay before delivering the event.
     * @return a cancellable representing the task
     */
    Cancellable schedule(Runnable job, Duration delay);

    Cancellable schedule(SipTimerListener listener, SipTimerEvent timerEvent, Duration delay);

}
