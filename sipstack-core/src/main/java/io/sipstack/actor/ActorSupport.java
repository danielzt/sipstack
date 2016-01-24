package io.sipstack.actor;

import org.slf4j.Logger;

import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 *
 * @param <T>
 * @param <S>
 */
public abstract class ActorSupport<T, S extends Enum<S>> implements Actor {

    /**
     * Our current currentState.
     */
    protected S currentState;

    /**
     * The functions implementing the current state.
     */
    private final Consumer<T>[] states;

    /**
     * The functions implementing the on enter actions.
     */
    private final Consumer<T>[] onEnterActions;

    /**
     * The functions implementing the on exit actions.
     */
    private final Consumer<T>[] onExitActions;

    private Consumer<T> alwaysAction;

    /**
     * Just a unique identifier for this actor and really only used for logging purposes.
     */
    private final String id;

    private T currentEvent;

    private final S terminalState;

    private ActorContext currentContext;

    protected ActorSupport(final String id, final S initialState, final S terminalState, final S ... values) {
        this.id = id;
        currentState = initialState;
        this.terminalState = terminalState;

        states = new Consumer[values.length];
        onEnterActions = new Consumer[values.length];
        onExitActions = new Consumer[values.length];
    }

    protected ActorContext ctx() {
        return currentContext;
    }

    /**
     * Always execute the function upon every event the actor receive.
     *
     * @param execute
     */
    protected void always(final Consumer<T> execute) {
        this.alwaysAction = execute;
    }

    /**
     * When in a particular currentState, perform the following function.
     *
     * @param state the currentState in quesiton.
     */
    protected void when(final S state, final Consumer<T> execute) {
        states[state.ordinal()] = execute;
    }

    // TODO: playing around with guards as well.
    protected void when(final S state, final Predicate<T> guard, final Consumer<T> execute) {
        states[state.ordinal()] = execute;
    }

    /**
     * When entering a particular currentState, perform the following action.
     *
     * @param state the currentState in question
     * @param action the action to execute when we enter the currentState
     */
    protected void onEnter(final S state, final Consumer<T> action) {
        onEnterActions[state.ordinal()] = action;
    }

    /**
     * When exiting a particular currentState, perform the following action.
     *
     * @param state the currentState in question
     * @param action the action to execute when we leave the currentState
     */
    protected void onExit(final S state, final Consumer<T> action) {
        onExitActions[state.ordinal()] = action;
    }

    @Override
    public final void onReceive(final ActorContext ctx, final Object msg) {
        try {
            currentEvent = (T) msg;
            currentContext = ctx;

            if (alwaysAction != null) {
                alwaysAction.accept(currentEvent);
            }

            if (states[currentState.ordinal()] != null) {
                states[currentState.ordinal()].accept(currentEvent);
            } else {
                // TODO: if we are in a state that doesn't have
                // a behavior defined we should do what?
                logger().warn("State \"{}\" is not defined. Message will be dropped", currentState);
            }
        } catch (final ClassCastException e) {
            // TODO: not expected class, handle it in some way
            e.printStackTrace();
        }
    }

    @Override
    public final boolean isTerminated() {
        return currentState == terminalState;
    }

    protected final void become(final S newState, final String msg) {
        // TODO: perhaps make it configurable for how the state transition is logged?
        if (msg == null) {
            logger().info("{} {} -> {}", this.id, currentState, newState);
        } else {
            logger().info("{} {} -> {} {}", this.id, currentState, newState, msg);
        }

        if (currentState != newState) {
            final Consumer<T> exitAction = onExitActions[currentState.ordinal()];
            if (exitAction != null) {
                exitAction.accept(currentEvent);
            }

            final Consumer<T> enterAction =  onEnterActions[newState.ordinal()];
            if (enterAction != null) {
                enterAction.accept(currentEvent);
            }
        }

        currentState = newState;

    }

    protected final void become(final S newState) {
        become(newState, null);
    }

    protected void unhandled(final T event) {
        logger().warn("{} {} Unhandled event \"{}\". Event will be dropped", this.id, this.currentState, event.getClass());
    }

    protected void logWarn(final String msg, Object ... args) {
        logger().warn("{} " + msg, this.id, args);
    }

    protected void logInfo(final String msg, Object ... args) {
        logger().info("{} " + msg, this.id, args);
    }

    public final S state() {
        return currentState;
    }

    protected abstract Logger logger();

}