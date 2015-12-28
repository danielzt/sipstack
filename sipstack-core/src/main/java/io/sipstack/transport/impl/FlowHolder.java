package io.sipstack.transport.impl;

import io.sipstack.netty.codec.sip.Connection;

/**
 * @author jonas@jonasborjesson.com
 */
public interface FlowHolder {

    FlowActor flow();

    Connection connection();

}
