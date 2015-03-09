/**
 * 
 */
package io.sipstack.event;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.Key;
import io.sipstack.netty.codec.sip.SipMessageEvent;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public class SipEvent implements Event {

    private final SipMessage msg;
    private final long arrivalTime;
    private final Key key;

    public static SipEvent create(final SipMessageEvent event) {
        final SipMessage msg = event.getMessage();
        final Key key = createKey(msg);
        return new SipEvent(key, event.getArrivalTime(), msg);
    }

    public static SipEvent create(final Key key, final SipResponse response) {
        // TODO: don't use System.currentTimeMillis
        return new SipEvent(key, System.currentTimeMillis(), response);
    }

    public static SipEvent create(final SipRequest request) {
        // TODO: don't use System.currentTimeMillis
        final Key key = createKey(request);
        return new SipEvent(key, System.currentTimeMillis(), request);
    }

    public static SipEvent create(final SipResponse response) {
        // TODO: don't use System.currentTimeMillis
        final Key key = createKey(response);
        return new SipEvent(key, System.currentTimeMillis(), response);
    }

    private static Key createKey(final SipMessage msg) {
        final Buffer callId = msg.getCallIDHeader().getValue();
        return Key.withBuffer(callId);
    }

    /**
     * 
     */
    public SipEvent(final Key key, final long arrivalTime, final SipMessage msg) {
        this.key = key;
        this.arrivalTime = arrivalTime;
        this.msg = msg;
    }

    @Override
    public Key key() {
        return this.key;
    }

    @Override
    public long getArrivalTime() {
        return this.arrivalTime;
    }

    public SipMessage getSipMessage() {
        return this.msg;
    }

}
