/**
 * 
 */
package io.sipstack.actor;

import io.pkts.buffer.Buffer;
import io.sipstack.netty.codec.sip.SipMessageEvent;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public class SipEvent implements Event {

    private final SipMessageEvent event;
    private final Key key;

    /**
     * 
     */
    public SipEvent(final SipMessageEvent event) {
        final Buffer callId = event.getMessage().getCallIDHeader().getValue();
        this.key = Key.withBuffer(callId);
        this.event = event;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isSipMessage() {
        return true;
    }

    @Override
    public Key key() {
        return this.key;
    }

}
