package io.sipstack.event;

import io.pkts.packet.sip.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 */
public interface IOWriteEvent<T> extends IOEvent<T> {

    @Override
    default boolean isIOReadEvent() {
        return true;
    }

    /**
     * Helper method to create a read event based off of a netty SipMessageEvent.
     * Typically, the TransportSupervisor will convert the raw sip message event
     * into an IOReadEvent instead, which the rest of the stack is processing.
     *
     * @return
     */
    static IOWriteEvent<SipMessage> create(final SipMessage msg) {
        // TODO: do not use System but rather a clock interface
        final long arrivalTime = System.currentTimeMillis();
        return new IOSipWriteEvent(arrivalTime, msg);
    }

    static final class IOSipWriteEvent extends IOEvent.BaseIOEvent<SipMessage> implements IOWriteEvent<SipMessage> {

        private final SipMessage msg;

        private IOSipWriteEvent(final long arrivalTime, final SipMessage msg) {
            super(arrivalTime);
            this.msg = msg;
        }

        @Override
        public boolean isSipIOEvent() {
            return true;
        }

        @Override
        public boolean isSipWriteEvent() {
            return true;
        }

        @Override
        public IOEvent<SipMessage> toSipIOEvent() {
            return this;
        }

        @Override
        public IOWriteEvent<SipMessage> toSipIOWriteEvent() {
            return this;
        }

        @Override
        public SipMessage getObject() {
            return msg;
        }
    }
}
