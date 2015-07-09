package io.sipstack.application.impl;

import io.pkts.packet.sip.SipMessage;
import io.sipstack.application.ApplicationContext;
import io.sipstack.application.ApplicationController;
import io.sipstack.transactionuser.Dialog;
import io.sipstack.transactionuser.TransactionUserEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public interface InternalApplicationContext extends ApplicationContext {

    /**
     * Will be called by the io.sipstack.application.application controller ({@link ApplicationController}) before
     * the actual io.sipstack.application.application is invoked.
     *
     * @param event
     */
    void preInvoke(TransactionUserEvent event);

    /**
     * Will be called by the io.sipstack.application.application controller after the actual
     * io.sipstack.application.application is invoked and at this point, the wishes of the
     * io.sipstack.application.application will be executed. E.g., when an io.sipstack.application.application asks
     * to proxy a request, that actually doesn't happen until control
     * has returned to the
     *
     */
    void postInvoke();
}
