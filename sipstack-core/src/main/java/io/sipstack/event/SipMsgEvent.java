/**
 * 
 */
package io.sipstack.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.actor.Key;
import io.sipstack.netty.codec.sip.SipMessageEvent;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public class SipMsgEvent implements Event {

    private final SipMessage msg;
    private final long arrivalTime;
    private final Key key;

    @Override
    public final boolean isSipMsgEvent() {
        return true;
    }

    public static SipMsgEvent create(final SipMessageEvent event) {
        final SipMessage msg = event.getMessage();
        final Key key = Key.withSipMessage(msg);
        return new SipMsgEvent(key, event.getArrivalTime(), msg);
    }

    public static SipMsgEvent create(final Key key, final long timeStamp, final SipMessage msg) {
        return new SipMsgEvent(key, timeStamp, msg);
    }

    public static SipMsgEvent create(final Key key, final SipResponse response) {
        // TODO: don't use System.currentTimeMillis
        return new SipMsgEvent(key, System.currentTimeMillis(), response);
    }

    public static SipMsgEvent create(final SipRequest request) {
        // TODO: don't use System.currentTimeMillis
        final Key key = Key.withSipMessage(request);
        return new SipMsgEvent(key, System.currentTimeMillis(), request);
    }

    public static SipMsgEvent create(final SipResponse response) {
        // TODO: don't use System.currentTimeMillis
        final Key key = Key.withSipMessage(response);
        return new SipMsgEvent(key, System.currentTimeMillis(), response);
    }

    @Override
    public SipMsgEvent toSipMsgEvent() {
        return this;
    }

    /**
     * 
     */
    public SipMsgEvent(final Key key, final long arrivalTime, final SipMessage msg) {
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
