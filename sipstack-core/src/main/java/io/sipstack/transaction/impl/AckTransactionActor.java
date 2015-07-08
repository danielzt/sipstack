package io.sipstack.transaction.impl;

import io.sipstack.actor.ActorSupport;
import io.sipstack.event.Event;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ACK to a 2xx response doesn't really have a transaction per se. Various RFC states that it goes
 * in its own transaction but that transaction doesn't really have any state associated with it etc etc.
 * However, to make the {@link io.sipstack.transaction.TransactionUser} interface consistent, we will
 * create a mini FSM representing an ACK transaction which dies right away.
 *
 * @author jonas@jonasborjesson.com
 */
public class AckTransactionActor extends ActorSupport<Event, TransactionState> implements TransactionActor {

    private static final Logger logger = LoggerFactory.getLogger(AckTransactionActor.class);

    private final TransactionId id;
    private final boolean isServerTransaction;

    protected AckTransactionActor(final TransactionId id, final boolean isServerTransaction) {
        super(id.toString(), TransactionState.INIT, TransactionState.TERMINATED, TransactionState.values());
        this.id = id;
        this.isServerTransaction = isServerTransaction;

        when(TransactionState.INIT, this::onInit);
    }

    /**
     * The init state. Just make sure that the first event we receive is the same INVITE as created
     * the transaction (yes, we compare references in this case, that's what we want) and then
     * transition over to the proceeding state.
     */
    private void onInit(final Event event) {
        if (event.isSipRequestEvent()) {
            if (isServerTransaction) {
                ctx().forwardUpstream(event);
            } else {
                ctx().forwardDownstream(event);
            }
        }
        become(TransactionState.TERMINATED);
    };


    @Override
    protected Logger logger() {
        return logger;
    }

    @Override
    public TransactionId id() {
        return id;
    }
}
