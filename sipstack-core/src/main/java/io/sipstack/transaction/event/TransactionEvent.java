package io.sipstack.transaction.event;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionState;
import io.sipstack.transaction.event.impl.SipRequestTransactionEventImpl;
import io.sipstack.transaction.event.impl.SipResponseTransactionEventImpl;
import io.sipstack.transaction.event.impl.TransactionTerminatedEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface TransactionEvent {

    Transaction transaction();

    /**
     * Check if this {@link TransactionEvent} is of type {@link SipTransactionEvent}.
     */
    default boolean isSipTransactionEvent() {
        return false;
    }

    default SipTransactionEvent toSipTransactionEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipTransactionEvent.class.getName());
    }

    /**
     * Check if this {@link TransactionEvent} is of type {@link SipRequestTransactionEvent}.
     */
    default boolean isSipRequestTransactionEvent() {
        return false;
    }

    default SipRequestTransactionEvent toSipRequestTransactionEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipRequestTransactionEvent.class.getName());
    }

    /**
     * Check if this {@link TransactionEvent} is of type {@link SipResponseTransactionEvent}.
     */
    default boolean isSipResponseTransactionEvent() {
        return false;
    }

    default SipResponseTransactionEvent toSipResponseTransactionEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + SipResponseTransactionEvent.class.getName());
    }

    /**
     * Check if this {@link TransactionEvent} is of type {@link TransactionLifeCycleEvent}.
     */
    default boolean isTransactionLifeCycleEvent() {
        return false;
    }

    default TransactionLifeCycleEvent toTransactionLifeCycleEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + TransactionLifeCycleEvent.class.getName());
    }

    /**
     * Check if this {@link TransactionEvent} is of type {@link SipResponseTransactionEvent}.
     */
    default boolean isTransactionTerminatedEvent() {
        return false;
    }

    default TransactionTerminatedEvent toTransactionTerminatedEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + TransactionTerminatedEvent.class.getName());
    }

    static SipTransactionEvent create(final Transaction transaction, final SipMessage msg) {
        if (msg.isRequest()) {
            return create(transaction, msg.toRequest());
        }

        return create(transaction, msg.toResponse());
    }

    /**
     *
     * @param transaction
     * @return
     */
    static TransactionLifeCycleEvent create(final Transaction transaction) {
        if (transaction.state() == TransactionState.TERMINATED) {
            return new TransactionTerminatedEventImpl(transaction);
        }

        throw new RuntimeException("havent' done the rest yet...");
    }

    static SipRequestTransactionEvent create(final Transaction transaction, final SipRequest request) {
        return new SipRequestTransactionEventImpl(transaction, request);
    }

    static SipResponseTransactionEvent create(final Transaction transaction, final SipResponse response) {
        return new SipResponseTransactionEventImpl(transaction, response);
    }

}
