package io.sipstack.transport.impl;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowFuture;

import java.util.function.Consumer;

/**
 * @author jonas@jonasborjesson.com
 */
public class FlowFutureImpl implements FlowFuture, GenericFutureListener<Future<Connection>> {

    private final Consumer<Flow> onSuccess;
    private final Consumer<Flow> onFailure;
    private final Consumer<Flow> onCancel;
    private final Future<Connection> actualFuture;

    public FlowFutureImpl(final Future<Connection> actualFuture,
                          final Consumer<Flow> onSuccess,
                          final Consumer<Flow> onFailure,
                          final Consumer<Flow> onCancel) {
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
    public void operationComplete(final Future<Connection> future) throws Exception {
        if (future.isSuccess() && onSuccess != null) {
            // TODO: need to save this flow
            final Connection connection = future.getNow();
            final Flow flow = new DefaultFlow(connection);
            onSuccess.accept(flow);
        } else if (future.isCancelled() && onCancel != null) {
            onCancel.accept(new CancelledFlow());
        } else if (future.cause() != null && onFailure != null) {
            onFailure.accept(new FailureFlow(future.cause()));
        }
    }
}
