package io.sipstack.netty.codec.sip.application;

import io.pkts.packet.sip.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 */
public interface InternalApplicationContext extends ApplicationContext {

    /**
     * Will be called by the application controller ({@link ApplicationController}) before
     * the actual application is invoked.
     *
     * @param message
     */
    void preInvoke(SipMessage message);

    /**
     * Will be called by the application controller after the actual
     * application is invoked and at this point, the wishes of the
     * application will be executed. E.g., when an application asks
     * to proxy a request, that actually doesn't happen until control
     * has returned to the
     *
     */
    void postInvoke();
}
