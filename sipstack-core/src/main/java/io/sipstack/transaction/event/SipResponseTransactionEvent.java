package io.sipstack.transaction.event;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipResponseTransactionEvent extends SipRequestTransactionEvent {

    @Override
    default boolean isSipResponseTransactionEvent() {
        return true;
    }

    @Override
    default SipResponseTransactionEvent toSipResponseTransactionEvent() {
        return this;
    }
}
