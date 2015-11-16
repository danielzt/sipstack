package io.sipstack.transport.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.UdpConnection;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowFuture;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

/**
 * @author jonas@jonasborjesson.com
 */
public class FlowFutureImpl implements FlowFuture, GenericFutureListener<ChannelFuture> {

    /**
     * A reference to all the existing flows and where we will store our flow if successful.
     */
    private final FlowStorage flowStorage;

    private final Consumer<Flow> onSuccess;
    private final Consumer<Flow> onFailure;
    private final Consumer<Flow> onCancel;
    private final ChannelFuture actualFuture;

    public FlowFutureImpl(final FlowStorage flowStorage,
                          final ChannelFuture actualFuture,
                          final Consumer<Flow> onSuccess,
                          final Consumer<Flow> onFailure,
                          final Consumer<Flow> onCancel) {
        this.flowStorage = flowStorage;
        this.actualFuture = actualFuture;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
        this.onCancel = onCancel;
    }

    @Override
    public boolean cancel() {
        return actualFuture.cancel(false);
    }

    @Override
    public void operationComplete(final ChannelFuture future) throws Exception {
        if (future.isSuccess() && onSuccess != null) {
            final Channel channel = future.channel();
            System.err.println("Ok, now we are bloody connected." + channel);

            // There is a chance that several tried to connect at the same time.
            // This takes care of that.
            // TODO: we should probably not pass in the raw hash map
            // TODO: we don't know if this is a UDP/TCP connection at this point. Needs to be fixed...
            // TODO: hmmm... now that we are catching the events bubbling up from the
            // TODO: lower levels can we be sure that a flow is created and as such as we only
            // TODO: need to do a "get" on the flowStorage??? Assume that "bind" etc is called before
            // TODO: this as well as the registered and active ones...
            final Connection connection = new UdpConnection(channel, (InetSocketAddress)channel.remoteAddress());
            final FlowActor actor = flowStorage.ensureFlow(connection);
            onSuccess.accept(actor.flow());
        } else if (future.isCancelled() && onCancel != null) {
            onCancel.accept(new CancelledFlow());
        } else if (future.cause() != null && onFailure != null) {
            System.err.println("What, it failed, why???");
            future.cause().printStackTrace();
            onFailure.accept(new FailureFlow(future.cause()));
        }
    }

}
