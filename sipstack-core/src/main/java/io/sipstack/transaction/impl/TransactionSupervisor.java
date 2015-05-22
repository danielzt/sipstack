/**
 * 
 */
package io.sipstack.transaction.impl;

import io.hektor.core.Actor;
import io.hektor.core.ActorRef;
import io.hektor.core.Props;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.event.Event;
import io.sipstack.event.IOEvent;
import io.sipstack.event.InitEvent;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 * 
 */
public class TransactionSupervisor implements Actor {

    private final Logger logger = LoggerFactory.getLogger(TransactionSupervisor.class);

    private final Map<TransactionId, TransactionActor> transactions = new HashMap<>(100, 0.75f);

    private final TransactionLayerConfiguration config;

    private ActorRef downstreamActor;

    private ActorRef upstreamActor;

    /**
     * 
     */
    public TransactionSupervisor(final TransactionLayerConfiguration config) {
        this.config = config;
    }

    /**
     * Get the {@link Transaction} for the given {@link TransactionId}.
     * 
     * Note: this can ONLY be called from the thread that normally handles transactions for this
     * supervisor. Calling this from another thread is not safe. Hence, ONLY the internal
     * implementation to sipstack should be using this method.
     * 
     * @param id
     * @return
     */
    public Transaction getTransaction(final TransactionId id) {
        final TransactionActor t = this.transactions.get(id);
        if (t != null) {
            return t.getTransaction();
        }

        return null;
    }

    public TransactionLayerConfiguration getConfig() {
        return this.config;
    }

    /*
    private TransactionActor ensureTransaction(final TransactionId id, final SipMsgEvent event) {
        final TransactionActor t = this.transactions.get(id);
        if (t != null) {
            return t;
        }

        // if this is an ACK and we didn't find a transaction for this
        // ACK that can only mean that this is an ACK to a 2xx response
        // and therefore this ACK doesn't really have a transaction (an
        // ACK goes in its own transaction for 2xx responses but ACK doesn't
        // expect a response so therefore we will not actually create a new
        // transaction for it)
        if (event.getSipMessage().isAck()) {
            return null;
        }

        final TransactionActor newTransaction = TransactionActor.create(this, id, event);
        final Optional<Throwable> exception = safePreStart(newTransaction);
        if (exception.isPresent()) {
            // TODO: do something about it... such as do not put it in the transactions table
            throw new RuntimeException("The actor threw an exception in PostStop and I havent coded that up yet",
                    exception.get());
        }
        this.transactions.put(id, newTransaction);
        return newTransaction;
    }
    */


    /**
    public void killChild(final Actor actor) {
        try {
            // can only be a TransactionActor
            final TransactionId id = ((TransactionActor) actor).getTransactionId();
            final TransactionActor transaction = this.transactions.remove(id);
            final Optional<Throwable> exception = safePostStop(transaction);
            if (exception.isPresent()) {
                // TODO: do something about it.
                throw new RuntimeException("The actor threw an exception in PostStop and I havent coded that up yet",
                        exception.get());
            }
        } catch (final ClassCastException e) {
            // strange...
            throw e;
        }
    }
     */


    @Override
    public void onReceive(final Object msg) {
        final Event event = (Event)msg;

        if (event.isSipIOEvent()) {
            final IOEvent<SipMessage> ioEvent = (IOEvent<SipMessage>)event.toIOEvent();
            final SipMessage sipMsg = ioEvent.getObject();
            final TransactionId id = TransactionId.create(sipMsg);
            final String idStr = id.toString();
            final Optional<ActorRef> child = ctx().child(idStr);

            final ActorRef transaction = child.orElseGet(() -> {

                // if this is an ACK and we didn't find a transaction for this
                // ACK that can only mean that this is an ACK to a 2xx response
                // and therefore this ACK doesn't really have a transaction (an
                // ACK goes in its own transaction for 2xx responses but ACK doesn't
                // expect a response so therefore we will not actually create a new
                // transaction for it)
                if (sipMsg.isAck()) {
                    return upstreamActor;
                }

                // There is also a chance that the transaction is gone and we have
                // received an inbound event for a response, which then
                // should have matched a server transaction.
                // TODO: deal with this.
                Class<? extends Actor> transactionClass = null;

                if (event.isIOReadEvent() && sipMsg.isRequest()) {
                    if (sipMsg.isInvite()) {
                        transactionClass = InviteServerTransactionActor.class;
                    } else {
                        transactionClass = NonInviteServerTransactionActor.class;
                    }
                }

                final Props props = Props.forActor(transactionClass)
                        .withConstructorArg(upstreamActor)
                        .withConstructorArg(id)
                        .withConstructorArg(ioEvent)
                        .withConstructorArg(config)
                        .build();
                return ctx().actorOf(idStr, props);
            });

            // forward the message
            transaction.tell(event, sender());
        } else if (event.isInitEvent()) {
            downstreamActor = ((InitEvent)event).downstreamSupervisor;
            upstreamActor = ((InitEvent)event).upstreamSupervisor;
        }
    }
}
