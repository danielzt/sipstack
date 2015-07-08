package io.sipstack.application;

import java.util.function.Consumer;

import io.netty.channel.ChannelHandlerContext;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.actor.InternalScheduler;
import io.sipstack.event.Event;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.Transport;
import io.sipstack.transaction.impl.DefaultTransactionLayer;
import io.sipstack.transactionuser.DefaultUA;
import io.sipstack.transactionuser.Dialog;
import io.sipstack.transactionuser.TransactionUserEvent;
import io.sipstack.transactionuser.TransactionUserLayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jonas@jonasborjesson.com
 */
public class ApplicationController implements Consumer<TransactionUserEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTransactionLayer.class);

    private final InternalScheduler scheduler;

    private final Clock clock;

    private final ApplicationInstanceCreator appCreator;

    private final ApplicationInstanceStore applicationStore;

    private TransactionUserLayer transactionUserLayer;

    public ApplicationController(final Clock clock, final InternalScheduler scheduler, final ApplicationInstanceCreator creator) {
        this.clock = clock;
        this.scheduler = scheduler;
        this.appCreator = creator;
        applicationStore = new DefaultApplicationInstanceStore(this, creator);
    }

    public void start(final TransactionUserLayer transactionUserLayer) {
        this.transactionUserLayer = transactionUserLayer;
    }

    private void invokeApplication(final ApplicationInstance app, final InternalApplicationContext appCtx, final TransactionUserEvent event) {

        // Note that it is utterly important that the lock
        // for both the io.sipstack.application.application and the io.sipstack.application.application context
        // is "on" for all interactions with these two objects.
        // The entire design are dependent on it!
        // Also note that we are using the io.sipstack.application.application context since
        // it is hopefully less of a risk that the user uses it as a mutext.
        // TODO: perhaps we should use something completely different to avoid the user
        // accidentally locking on the same thing.
        synchronized(appCtx) {
            app._ctx.set(appCtx);
            try {
                appCtx.preInvoke(event);
                final SipMessage message = event.message();
                if (message.isRequest()) {
                    app.onRequest(message.toRequest());
                } else {
                    app.onResponse(message.toResponse());
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

    public void send(final DefaultUA ua, final SipMessage message) {
        final Dialog dialog = transactionUserLayer.findOrCreateDialog(message);
        dialog.setConsumer(ua);
        dialog.send(message);
    }

    @Override
    public void accept(final TransactionUserEvent event) {
        final ApplicationInstance app = applicationStore.ensureApplication(event.message());
        final InternalApplicationContext appContext = applicationStore.ensureApplicationContext(app.id());
        invokeApplication(app, appContext, event);
    }
}
