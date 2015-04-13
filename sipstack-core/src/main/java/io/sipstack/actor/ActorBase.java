package io.sipstack.actor;

import io.sipstack.event.Event;
import org.slf4j.Logger;

import java.util.function.BiConsumer;

/**
 * Created by jonas on 3/27/15.
 */
public abstract class ActorBase<S extends Enum<S>> implements Actor {

    /**
     * Our current currentState.
     */
    protected S currentState;

    /**
     * The functions implementing the current state.
     */
    private final BiConsumer<ActorContext, Event>[] states;

    /**
     * The functions implementing the on enter actions.
     */
    private final BiConsumer<ActorContext, Event>[] onEnterActions;

    /**
     * The functions implementing the on exit actions.
     */
    private final BiConsumer<ActorContext, Event>[] onExitActions;

    /**
     * Just a unique identifier for this actor and really only used for logging purposes.
     */
    private final String id;

    /**
     * Whenever {@link #onEnter(Enum, BiConsumer)} is invoked, we will save the latest {@link ActorContext}
     * and {@link Event} here so that we allow the sub-class to e.g. call {@link #become(Enum)} without having
     * to actually pass in this stuff itself.
     */
    private ActorContext currentCtx;

    private Event currentEvent;

    /**
     * @param values
     */
    protected ActorBase(final String id, final S initialState, final S[] values) {
        this.id = id;
        currentState = initialState;
        states = new BiConsumer[values.length];
        onEnterActions = new BiConsumer[values.length];
        onExitActions = new BiConsumer[values.length];
    }

    /**
     * When in a particular currentState, perform the following function.
     *
     * @param state the currentState in quesiton.
     */
    protected void when(final S state, BiConsumer<ActorContext, Event> execute) {
        states[state.ordinal()] = execute;
    }

    /**
     * When entering a particular currentState, perform the following action.
     *
     * @param state the currentState in question
     * @param action the action to execute when we enter the currentState
     */
    protected void onEnter(S state, BiConsumer<ActorContext, Event> action) {
        onEnterActions[state.ordinal()] = action;
    }

    /**
     * When exiting a particular currentState, perform the following action.
     *
     * @param state the currentState in question
     * @param action the action to execute when we leave the currentState
     */
    protected void onExit(S state, BiConsumer<ActorContext, Event> action) {
        onExitActions[state.ordinal()] = action;
    }

    @Override
    public final void onEvent(final ActorContext ctx, final Event event) {
        currentCtx = ctx;
        currentEvent = event;
        states[currentState.ordinal()].accept(ctx, event);
    }

    protected final void become(final S newState) {
        logger().info("{} {} -> {}", this.id, currentState, newState);

        if (currentState != newState) {
            final BiConsumer<ActorContext, Event> exitAction = onExitActions[currentState.ordinal()];
            if (exitAction != null) {
                exitAction.accept(currentCtx, currentEvent);
            }

            final BiConsumer<ActorContext, Event> enterAction =  onEnterActions[newState.ordinal()];
            if (enterAction != null) {
                enterAction.accept(currentCtx, currentEvent);
            }
        }

        currentState = newState;
    }

    protected final S state() {
        return currentState;
    }

    @Override
    public ActorRef self() {
        // TODO Auto-generated method stub
        return null;
    }

    protected abstract Logger logger();


}
