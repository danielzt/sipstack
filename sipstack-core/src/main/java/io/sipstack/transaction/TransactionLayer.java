package io.sipstack.transaction;

import io.netty.channel.ChannelHandlerContext;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.sipstack.actor.InternalScheduler;
import io.sipstack.actor.SingleContext;
import io.sipstack.config.TransactionLayerConfiguration;
import io.sipstack.core.SipLayer;
import io.sipstack.event.Event;
import io.sipstack.event.SipTimerEvent;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.transactionuser.DefaultTransactionUser;
import io.sipstack.transport.Flow;
import io.sipstack.transport.TransportLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public class TransactionLayer implements SipLayer, TransactionFactory {

    private static final Logger logger = LoggerFactory.getLogger(TransactionLayer.class);

    private final TransactionLayerConfiguration config;

    private final InternalScheduler scheduler;

    private final Clock clock;

    private final TransactionStore transactionStore;

    /**
     * All {@link Transaction}s will forward all their events to a particular
     * {@link TransactionUser}, which by default is the {@link DefaultTransactionUser}.
     */
    private TransactionUser defaultTransactionListener;

    private TransportLayer transportLayer;

    public TransactionLayer(final Clock clock, final InternalScheduler scheduler, final TransactionLayerConfiguration config) {
        this.clock = clock;
        this.scheduler = scheduler;
        this.config = config;
        transactionStore = new DefaultTransactionStore(this, config);
    }

    public void useTransportLayer(final TransportLayer transportLayer) {
        this.transportLayer = transportLayer;
    }

    public void defaultTransactionUser(final TransactionUser tu) {
        this.defaultTransactionListener = tu;
    }


    @Override
    public void onUpstream(final Flow flow, final SipMessage msg) {
        final DefaultTransactionHolder holder = (DefaultTransactionHolder)transactionStore.ensureTransaction(msg);
        try {
            invoke(flow, msg, holder);
            checkIfTerminated(holder);
        } catch (final ClassCastException e) {
            // strange...
            logger.warn("Got a unexpected message of type {}. Will ignore.", msg.getClass());
        }
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

    private void invoke(final Flow flow, final SipMessage msg, final DefaultTransactionHolder holder) {
        if (holder == null) {
            return;
        }

        try {
            final SingleContext actorCtx = invokeTransaction(flow, msg, holder);
            actorCtx.downstream().ifPresent(e -> {
                if (holder.flow().isPresent()) {
                    holder.flow().get().write(e);
                } else {
                    throw new RuntimeException("TODO: need to create a flow here or something");
                }
            });

            // TODO: I wonder how we are going to prevent upstream
            // actors from writing straight to the socket. We could have the
            // DefaultFlow.write() actually buffer somehow using another
            // contextual ThreadLocal whatever. Also, same goes for creating
            // new Transactions. We probably want to provide another contextual
            // ThreadLocal called e.g. Transactions where the TransactionUser implementation
            // can do Transactions.newClientTransaction(request).send();
            // Perhaps a way to also set the "listener" such as Transactions.newClientTransaction(myDialog, request).send();
            // where the dialog would then implement TransactionUser.
            //
            // No need for a newServerTransaction since that happens automatically but
            // perhaps we should do what jain sip does there as well?
            actorCtx.upstream().ifPresent(e -> {
                if (msg.isRequest()) {
                    holder.tu.onRequest(holder, msg.toRequest());
                } else {
                    holder.tu.onResponse(holder, msg.toResponse());
                }
            });
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }

    public void processSipTimerEvent(final ChannelHandlerContext ctx, final SipTimerEvent event) {
        throw new RuntimeException("Can't process timer events right now");
        /*
        try {
            final TransactionId id = (TransactionId) event.key();
            final TransactionActor transaction = transactionStore.get(id);
            if (transaction != null) {
                invoke(ctx, event, transaction);
                checkIfTerminated(transaction);
            }

        } catch (final ClassCastException e) {
            // TODO
            e.printStackTrace();
        }
        */
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
            transactionStore.remove(actor.id());
            // TODO: an actor can emit more events here.
            actor.stop();
            actor.postStop();
        }
    }

    /**
     *
     * @param event
     * @param transaction
     * @return
     */
    private SingleContext invokeTransaction(final Flow flow,
                                            final SipMessage msg,
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

                    // the flow may change so keep it up to date
                    if (holder.flow != flow) {
                        holder.flow = flow;
                    }

                    transaction.onReceive(ctx, Event.create(msg));
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
            ctx.forwardUpstream(Event.create(msg));
        }
        return ctx;
    }

    @Override
    public TransactionHolder createInviteServerTransaction(final TransactionId id, final SipRequest request, final TransactionLayerConfiguration config) {
        final TransactionActor actor = new InviteServerTransactionActor(id, request, config);
        return new DefaultTransactionHolder(defaultTransactionListener, actor);
    }

    @Override
    public TransactionHolder createNonInviteServerTransaction(final TransactionId id, final SipRequest request, final TransactionLayerConfiguration config) {
        final TransactionActor actor = new NonInviteServerTransactionActor(id, request, config);
        return new DefaultTransactionHolder(defaultTransactionListener, actor);
    }

    /**
     *
     */
    private class DefaultTransactionHolder implements TransactionHolder {

        private final TransactionActor actor;

        /**
         * It is this listener that will be receiving all {@link SipMessage}s
         * as well as any updates when a transaction has been created/destroyed
         * etc.
         */
        private final TransactionUser tu;

        private Flow flow;

        private DefaultTransactionHolder(TransactionUser tu, final TransactionActor actor) {
            this.tu = tu;
            this.actor = actor;
        }

        @Override
        public TransactionId id() {
            return actor.id();
        }

        @Override
        public void send(final SipMessage msg) {
            // TODO: may want to delay the writing here...
            flow.write(msg);
        }

        @Override
        public TransactionActor actor() {
            return actor;
        }

        @Override
        public Optional<Flow> flow() {
            return Optional.ofNullable(flow);
        }
    }



}
