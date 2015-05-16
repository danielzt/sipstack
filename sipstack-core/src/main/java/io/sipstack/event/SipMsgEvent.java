/**
 * 
 */
package io.sipstack.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.netty.codec.sip.SipMessageEvent;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public class SipMsgEvent implements Event {

    private final SipMessage msg;
    private final long arrivalTime;
    // private final Key key;

    @Override
    public final boolean isSipMsgEvent() {
        return true;
    }

    public static SipMsgEvent create(final SipMessageEvent event) {
        final SipMessage msg = event.getMessage();
        // final Key key = Key.withSipMessage(msg);
        // return new SipMsgEvent(key, event.getArrivalTime(), msg);
        return new SipMsgEvent(event.getArrivalTime(), msg);
    }

    // public static SipMsgEvent create(final Key key, final long timeStamp, final SipMessage msg) {
    public static SipMsgEvent create(final long timeStamp, final SipMessage msg) {
        return new SipMsgEvent(timeStamp, msg);
    }

    public static SipMsgEvent create(final SipResponse response) {
        // TODO: don't use System.currentTimeMillis
        return new SipMsgEvent(System.currentTimeMillis(), response);
    }

    public static SipMsgEvent create(final SipRequest request) {
        // TODO: don't use System.currentTimeMillis
        // final Key key = Key.withSipMessage(request);
        return new SipMsgEvent(System.currentTimeMillis(), request);
    }

    @Override
    public SipMsgEvent toSipMsgEvent() {
        return this;
    }

    /**
     * 
     */
    public SipMsgEvent(final long arrivalTime, final SipMessage msg) {
        this.arrivalTime = arrivalTime;
        this.msg = msg;
    }

    @Override
    public long getArrivalTime() {
        return this.arrivalTime;
    }

    public SipMessage getSipMessage() {
        return this.msg;
    }

}
