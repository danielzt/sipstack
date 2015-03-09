/**
 * 
 */
package io.sipstack.transport;

import io.sipstack.netty.codec.sip.ConnectionId;

/**
 * @author jonas
 *
 */
public interface Flow {

    ConnectionId getConnectionId();

}
