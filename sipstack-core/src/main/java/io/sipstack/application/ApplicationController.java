package io.sipstack.application;

import io.sipstack.actor.InternalScheduler;
import io.sipstack.application.impl.DefaultApplicationInstanceStore;
import io.sipstack.application.impl.DefaultSipRequestEvent;
import io.sipstack.application.impl.DefaultSipResponseEvent;
import io.sipstack.application.impl.InternalApplicationContext;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.transaction.impl.DefaultTransactionLayer;
import io.sipstack.transactionuser.TransactionEvent;
import io.sipstack.transactionuser.TransactionUserLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * @author jonas@jonasborjesson.com
 */
public class ApplicationController implements Consumer<TransactionEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTransactionLayer.class);

    private final InternalScheduler scheduler;

    private final Clock clock;

    private final ApplicationInstanceCreator creator;

    private TransactionUserLayer tu;

    private ApplicationInstanceStore applicationStore;

    public ApplicationController(final Clock clock, final InternalScheduler scheduler, final ApplicationInstanceCreator creator) {
        this.clock = clock;
        this.scheduler = scheduler;
        this.creator = creator;
    }

    public void start(final TransactionUserLayer tu) {
        applicationStore = new DefaultApplicationInstanceStore(tu, creator);
    }

    private void invokeApplication(final ApplicationInstance app, final InternalApplicationContext appCtx, final TransactionEvent tx) {

        // Note that it is utterly important that the lock
        // for both the .sipstack.application.application and the io.sipstack.application.application context
        // is "on" for all interactions with these two objects.
        // The entire design are dependent on it!
        // Also note that we are using the io.sipstack.application.application context since
        // it is hopefully less of a risk that the user uses it as a mutext.
        // TODO: perhaps we should use something completely different to avoid the user
        // accidentally locking on the same thing.
        synchronized(appCtx) {
            app._ctx.set(appCtx);
            try {
                appCtx.preInvoke(tx);
                if (tx.message().isRequest()) {
                    app.onRequest(new DefaultSipRequestEvent(tx.transaction(), tx.message().toRequest()));
                } else {
                    app.onResponse(new DefaultSipResponseEvent(tx.transaction(), tx.message().toResponse()));
                }
            } catch (final Throwable t) {
                t.printStackTrace();
            }
            app._ctx.remove();
            try {
                appCtx.postInvoke();
            } catch (final Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public void accept(final TransactionEvent event) {
        final ApplicationInstance app = applicationStore.ensureApplication(event.message());
        final InternalApplicationContext appContext = applicationStore.ensureApplicationContext(app.id());
        invokeApplication(app, appContext, event);
    }
}
