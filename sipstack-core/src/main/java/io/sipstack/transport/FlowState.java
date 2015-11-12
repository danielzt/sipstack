package io.sipstack.transport;

/**
 * Represents all the states a flow may be in,
 *
 * @author jonas@jonasborjesson.com
 */
public enum FlowState {
    INIT, READY, ACTIVE, WAIT_PONG, CLOSING, CLOSED;
}
