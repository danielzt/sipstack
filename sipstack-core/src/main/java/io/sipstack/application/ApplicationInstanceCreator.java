package io.sipstack.application;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ApplicationInstanceCreator {

    /**
     * Calculate the unique identifier representing the application instance that
     * should handle this message.
     *
     * @param message
     * @return
     */
    Buffer getId(SipMessage message);

    /**
     * Create a new instance of an {@link ApplicationInstance} where the id is what
     * was returned from {@link ApplicationInstanceCreator#getId(SipMessage)}.
     *
     * @param id
     * @param message
     * @return
     */
    ApplicationInstance createInstance(Buffer id, SipMessage message);
}
