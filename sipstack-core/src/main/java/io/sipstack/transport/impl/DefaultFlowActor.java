package io.sipstack.transport.impl;

import io.sipstack.actor.ActorSupport;
import io.sipstack.config.FlowConfiguration;
import io.sipstack.event.Event;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.event.IOEvent;
import io.sipstack.transport.Flow;
import io.sipstack.transport.FlowId;
import io.sipstack.transport.FlowState;
import io.sipstack.transport.event.FlowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultFlowActor extends ActorSupport<IOEvent, FlowState> implements FlowActor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFlowActor.class);

    private final FlowConfiguration config;

    private final Connection connection;

    private final FlowId flowId;

    protected DefaultFlowActor(final FlowConfiguration config, final Connection connection) {
        super(connection.id().toString(), FlowState.INIT, FlowState.CLOSED, FlowState.values());
        this.config = config;
        this.connection = connection;
        this.flowId = FlowId.create(connection.id());

        when(FlowState.INIT, this::onInit);

        onEnter(FlowState.READY, this::onEnterReady);
        when(FlowState.READY, this::onReady);
    }

    private void onInit(final IOEvent event) {
        become(FlowState.READY);
    }

    private void onEnterReady(final IOEvent event) {
        System.err.println("OnEnter stuff");
    }

    private void onReady(final IOEvent event) {
        System.err.println("Ready stuff");
    }

    @Override
    protected Logger logger() {
        return logger;
    }

    @Override
    public Flow flow() {
        return new DefaultFlow(connection);
    }

    @Override
    public FlowId id() {
        return flowId;
    }
}
