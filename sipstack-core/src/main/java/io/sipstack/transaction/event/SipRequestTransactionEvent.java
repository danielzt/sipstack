package io.sipstack.transaction.event;

/**
 * @author jonas@jonasborjesson.com
 */
public interface SipRequestTransactionEvent extends SipTransactionEvent {

    @Override
    default boolean isSipRequestTransactionEvent() {
        return true;
    }

    @Override
    default SipRequestTransactionEvent toSipRequestTransactionEvent() {
        return this;
    }
}
