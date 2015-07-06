package io.sipstack.application;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ApplicationInstanceStore {

    /**
     * Get or create method.
     *
     * @param msg
     * @return
     */
    ApplicationInstance ensureApplication(SipMessage msg);

    InternalApplicationContext ensureApplicationContext(Buffer appId);

    ApplicationInstance get(Buffer id);

    void remove(String id);

}
