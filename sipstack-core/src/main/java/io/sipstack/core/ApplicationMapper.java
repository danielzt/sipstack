package io.sipstack.core;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ApplicationMapper {

    /**
     * Map a SipMsgEvent to an application. The returned buffer will be used as a key for
     * an Application Actor. By default, the Call ID of the sip message is used.
     *
     * @param event
     * @return
     */
    // default Key map(SipMsgEvent event) {
        // return Key.withSipMessage(event.getSipMessage());
    // }
}
