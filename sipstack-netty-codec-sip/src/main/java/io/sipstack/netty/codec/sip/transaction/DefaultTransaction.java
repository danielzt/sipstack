package io.sipstack.netty.codec.sip.transaction;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultTransaction implements Transaction {

    private final TransactionId id;
    private final TransactionState state;

    public DefaultTransaction(final TransactionId id, final TransactionState state) {
        this.id = id;
        this.state = state;
    }

    @Override
    public TransactionId id() {
        return id;
    }

    @Override
    public TransactionState state() {
        return state;
    }
}
