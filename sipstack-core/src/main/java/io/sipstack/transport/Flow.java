/**
 * 
 */
package io.sipstack.transport;

import io.sipstack.netty.codec.sip.ConnectionId;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Flow {

    ConnectionId getConnectionId();

}
