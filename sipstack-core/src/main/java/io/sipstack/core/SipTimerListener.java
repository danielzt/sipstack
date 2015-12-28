package io.sipstack.core;

import io.sipstack.event.SipTimerEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipTimerListener {

    void onTimeout(SipTimerEvent timer);
}
