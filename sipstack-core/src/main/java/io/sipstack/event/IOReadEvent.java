/**
 * 
 */
package io.sipstack.event;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.event.SipMessageEvent;

/**
 * An event representing a read event.
 * 
 * @author jonas@jonasborjesson.com
 */
public interface IOReadEvent<T> extends IOEvent<T> {

    @Override
    default boolean isIOReadEvent() {
        return true;
    }

    /**
     * Helper method to create a read event based off of a netty SipMessageEvent.
     * Typically, the TransportSupervisor will convert the raw sip message event
     * into an IOReadEvent instead, which the rest of the stack is processing.
     *
     * @param event
     * @return
     */
    static IOReadEvent<SipMessage> create(final SipMessageEvent event) {
        final long arrivalTime = event.arrivalTime();
        final SipMessage msg = event.message();
        return new IOSipReadEvent(arrivalTime, msg);
    }

    static final class IOSipReadEvent extends IOEvent.BaseIOEvent<SipMessage> implements IOReadEvent<SipMessage> {

        private final SipMessage msg;

        private IOSipReadEvent(final long arrivalTime, final SipMessage msg) {
            super(arrivalTime);
            this.msg = msg;
        }

        @Override
        public boolean isSipIOEvent() {
            return true;
        }

        @Override
        public boolean isSipReadEvent() {
            return true;
        }

        @Override
        public IOEvent<SipMessage> toSipIOEvent() {
            return this;
        }

        @Override
        public IOReadEvent<SipMessage> toSipIOReadEvent() {
            return this;
        }

        @Override
        public SipMessage getObject() {
            return msg;
        }

    }

}
