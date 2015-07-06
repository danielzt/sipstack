package io.sipstack.netty.codec.sip.application;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.pkts.packet.sip.SipMessage;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.InboundOutboundHandlerAdapter;
import io.sipstack.netty.codec.sip.actor.InternalScheduler;
import io.sipstack.netty.codec.sip.event.Event;
import io.sipstack.netty.codec.sip.event.SipMessageEvent;
import io.sipstack.netty.codec.sip.transaction.TransactionLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jonas@jonasborjesson.com
 */
public class ApplicationController extends InboundOutboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionLayer.class);

    private final InternalScheduler scheduler;

    private final Clock clock;

    private final ApplicationInstanceCreator appCreator;

    private final ApplicationInstanceStore applicationStore;

    public ApplicationController(final Clock clock, final InternalScheduler scheduler, final ApplicationInstanceCreator creator) {
        this.clock = clock;
        this.scheduler = scheduler;
        this.appCreator = creator;
        applicationStore = new DefaultApplicationInstanceStore(creator);
    }

    /**
     * We only expect {@link SipMessageEvent}s here since there will always be a
     * decoder in-front of this one.
     */
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        // processEvent(ctx, msg);
        final Event event = (Event)msg;
        if (event.isSipMessageEvent()) {
            final SipMessageEvent sipMsgEvent = event.toSipMessageEvent();
            final SipMessage message = sipMsgEvent.message();
            final Connection connection = sipMsgEvent.connection();
            if (message.isRequest() && !message.isAck()) {
                ctx.writeAndFlush(new SipMessageEvent(connection, message.createResponse(200), 0));
            } else if (message.isAck()) {
                System.err.println("Ok, I did get the ACK");
            }
        }
    }

    /**
     * From ChannelOutboundHandler
     */
    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        processEvent(ctx, msg);
    }

    private void processEvent(final ChannelHandlerContext ctx, final Object msg) {
        try {
            final Event event = (Event)msg;
            final SipMessage sipMsg = event.toSipMessageEvent().message();
            final ApplicationInstance app = applicationStore.ensureApplication(sipMsg);
            final InternalApplicationContext appContext = applicationStore.ensureApplicationContext(app.id());
            invokeApplication(app, appContext, sipMsg);
        } catch (final ClassCastException e) {
            // strange...
            logger.warn("Got a unexpected message of type {}. Will ignore.", msg.getClass());
        }
    }

    private void invokeApplication(final ApplicationInstance app, final InternalApplicationContext appCtx, final SipMessage message) {

        // Note that it is utterly important that the lock
        // for both the application and the application context
        // is "on" for all interactions with these two objects.
        // The entire design are dependent on it!
        // Also note that we are using the application context since
        // it is hopefully less of a risk that the user uses it as a mutext.
        // TODO: perhaps we should use something completely different to avoid the user
        // accidentally locking on the same thing.
        synchronized(appCtx) {
            app._ctx.set(appCtx);
            try {
                appCtx.preInvoke(message);
                app.onRequest(message.toRequest());
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
}
