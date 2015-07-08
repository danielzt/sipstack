package io.sipstack.transaction.impl;

import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.sipstack.actor.InternalScheduler;
import io.sipstack.actor.SingleContext;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.core.SipTimerListener;
import io.sipstack.event.Event;
import io.sipstack.event.SipTimerEvent;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.transaction.Transaction;
import io.sipstack.transaction.TransactionId;
import io.sipstack.transaction.TransactionState;
import io.sipstack.transaction.TransactionUser;
import io.sipstack.transaction.Transactions;
import io.sipstack.transactionuser.DefaultTransactionUser;
import io.sipstack.transport.Flow;
import io.sipstack.transport.TransportUser;
import io.sipstack.transport.Transports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultTransactionLayer implements TransportUser, Transactions, TransactionFactory, SipTimerListener {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTransactionLayer.class);

    private final TransactionLayerConfiguration config;

    private final InternalScheduler scheduler;

    private final Clock clock;

    private final TransactionStore transactionStore;

    /**
     * All {@link Transaction}s will forward all their events to a particular
     * {@link TransactionUser}, which by default is the {@link DefaultTransactionUser}.
     */
    private TransactionUser defaultTransactionListener;

    private Transports transports;

    public DefaultTransactionLayer(final Clock clock, final InternalScheduler scheduler, final TransactionUser tu, final TransactionLayerConfiguration config) {
        this.clock = clock;
        this.scheduler = scheduler;
        this.config = config;
        transactionStore = new DefaultTransactionStore(this, config);
        this.defaultTransactionListener = tu;
    }

    public void start(final Transports transports) {
        this.transports = transports;
    }

    @Override
    public void onMessage(final Flow flow, final SipMessage msg) {
        // the onMessage method is the API the transaction layer implements to interact
        // with the underlying transport layer, hence, any message we recieve here
        // is going up the stack, hence the value true on "ensureTransaction"
        final DefaultTransactionHolder holder = (DefaultTransactionHolder)transactionStore.ensureTransaction(true, flow, msg);
        try {
            invoke(flow, Event.create(msg), holder);
            checkIfTerminated(holder);
        } catch (final ClassCastException e) {
            // strange...
            logger.warn("Got a unexpected message of type {}. Will ignore.", msg.getClass());
        }
    }

    @Override
    public void onWriteCompleted(Flow flow, SipMessage msg) {
        System.err.println("Yay, the write completed for message " + msg);

    }

    @Override
    public void onWriteFailed(Flow flow, SipMessage msg) {
        System.err.println("Nooooo, the write failed for message " + msg);
    }

    /*
    public Optional<Transaction> getTransaction(final TransactionId id) {
        final TransactionActor actor = transactionStore.get(id);
        if (actor != null) {
            return Optional.of(actor.transaction());
        }

        return Optional.empty();
    }
    */

    private void invoke(final Flow flow, final Event event, final DefaultTransactionHolder holder) {
        if (holder == null) {
            return;
        }

        try {
            final SingleContext actorCtx = invokeTransaction(flow, event, holder);
            actorCtx.downstream().ifPresent(e -> {
                transports.write(holder.flow(), e.getSipMessage());
            });

            actorCtx.upstream().ifPresent(e -> {
                if (e.isSipRequestEvent()) {
                    defaultTransactionListener.onRequest(holder, e.request());
                } else if (e.isSipResponseEvent()){
                    defaultTransactionListener.onResponse(holder, e.response());
                } else {
                    throw new RuntimeException("not sure how to forward this event upstream " + e);
                }
            });
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void onTimeout(SipTimerEvent timer) {
        try {
            final TransactionId id = (TransactionId) timer.key();
            final DefaultTransactionHolder holder = (DefaultTransactionHolder)transactionStore.get(id);
            if (holder != null) {
                invoke(null, timer, holder);
                checkIfTerminated(holder);
            }

        } catch (final ClassCastException e) {
            // TODO
            e.printStackTrace();
        }
    }

    /**
     * If an actor has been terminated then we will clean it up.
     *
     * @param actor
     */
    private void checkIfTerminated(final DefaultTransactionHolder holder) {
        if (holder == null) {
            return;
        }

        final TransactionActor actor = holder.actor;
        if (actor.isTerminated()) {
            // TODO: because the response is sent out
            // before the onUpstream has returned this will
            // be called twice now... not great. Do a
            // throw new RuntimeException() and you'll see where it is coming from
            transactionStore.remove(actor.id());
            // TODO: an actor can emit more events here.
            actor.stop();
            actor.postStop();
            defaultTransactionListener.onTransactionTerminated(holder);
        }
    }

    /**
     *
     * @param event
     * @param transaction
     * @return
     */
    private SingleContext invokeTransaction(final Flow flow,
                                            final Event event,
                                            final DefaultTransactionHolder holder) {

        final TransactionActor transaction = holder.actor;
        final SingleContext ctx = new SingleContext(clock, scheduler, transaction != null ? transaction.id() : null, this);
        if (transaction != null) {
            // Note, the synchronization model for everything within the core
            // sip stack is that you can ONLY hold one lock at a time and
            // you will ALWAYS synchronize on the actor itself. As long as
            // an actor does not try and lock something else, this should be
            // safe. However, breaking those rules and there is a good chance
            // of deadlock so if there ever is a need to have a more complicated
            // synchronization approach, then we should use another form of lock
            // that can timeout if we are not able to aquire the lock within a certain
            // time...
            synchronized (transaction) {
                try {

                    //
                    // TODO: not sure we should do this anymore, which also means we don't need to
                    // pass in the flow on the invokeTransaction
                    //
                    // the flow may change so keep it up to date
                    // if (holder.flow != flow && flow != null) {
                        // holder.flow = flow;
                    // }

                    transaction.onReceive(ctx, event);
                } catch (final Throwable t) {
                    // TODO: if the actor throws an exception we should
                    // do what?
                    t.printStackTrace();;
                }
            }
        } else {
            // if there were no transaction, such as for a stray response
            // or an ACK to a 2xx invite response, then it should be
            // forwarded upstream so simply pretend we invoked and
            // actor which asked to do just that.
            ctx.forwardUpstream(event);
        }
        return ctx;
    }

    @Override
    public TransactionHolder createInviteServerTransaction(final TransactionId id, final Flow flow, final SipRequest request, final TransactionLayerConfiguration config) {
        final TransactionActor actor = new InviteServerTransactionActor(id, request, config);
        return new DefaultTransactionHolder(flow, actor);
    }

    @Override
    public TransactionHolder createInviteClientTransaction(final TransactionId id, final Flow flow, final SipRequest request, final TransactionLayerConfiguration config) {
        final TransactionActor actor = new InviteClientTransactionActor(id, request, config);
        return new DefaultTransactionHolder(flow, actor);
    }

    @Override
    public TransactionHolder createNonInviteServerTransaction(final TransactionId id, final Flow flow, final SipRequest request, final TransactionLayerConfiguration config) {
        final TransactionActor actor = new NonInviteServerTransactionActor(id, request, config);
        return new DefaultTransactionHolder(flow, actor);
    }

    @Override
    public TransactionHolder createAckTransaction(final TransactionId id, final boolean isServer,
                                                  final Flow flow, final SipRequest request,
                                                  final TransactionLayerConfiguration config) {
        final TransactionActor actor = new AckTransactionActor(id, isServer);
        return new DefaultTransactionHolder(flow, actor);
    }

    @Override
    public Transaction send(final Flow flow, final SipMessage msg) {
        // note, the "send" method is exposed by our "north facing" API hence the
        // direction of the message is "down" hence the boolean value false to ensureTransaction
        final DefaultTransactionHolder holder = (DefaultTransactionHolder)transactionStore.ensureTransaction(false, flow, msg);
        try {
            invoke(flow, Event.create(msg), holder);
            checkIfTerminated(holder);
        } catch (final ClassCastException e) {
            // strange...
            logger.warn("Got a unexpected message of type {}. Will ignore.", msg.getClass());
        }
        return holder;
    }


    // @Override
    public Transaction send(final SipMessage msg) {
        // not allowed or should we allow this then to find an existing
        // transaction and if there is one it will be used and since a flow
        // always will be available on a transaction, we should be fine...
        throw new RuntimeException("Not allowed anymore!!!");
    }

    @Override
    public Flow.Builder createFlow(String host) throws IllegalArgumentException {
        return transports.createFlow(host);
    }

    /**
     *
     */
    private class DefaultTransactionHolder implements TransactionHolder {

        private final TransactionActor actor;

        private Flow flow;

        private DefaultTransactionHolder(final Flow flow, final TransactionActor actor) {
            this.flow = flow;
            this.actor = actor;
        }

        @Override
        public TransactionId id() {
            return actor.id();
        }

        @Override
        public TransactionState state() {
            return actor.state();
        }

        @Override
        public TransactionActor actor() {
            return actor;
        }

        @Override
        public Flow flow() {
            return flow;
        }

    }


}
