package io.sipstack.application;

import io.pkts.packet.sip.address.SipURI;
import io.sipstack.transactionuser.Proxy;

import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ApplicationContext {

    Optional<Proxy> getProxyByName(String friendlyName);

    /**
     * Create a new proxy builder with a referenceable friendly name.
     * Use this friendly name to lookup this proxy object later.
     *
     * Note, each Proxy object is always ONLY local to the ApplicationInstance
     * that created it, which means that the name only has to be unique to
     * each ApplicationInstance.
     *
     * @param name
     * @return
     */
    Proxy.Builder proxyWithFriendlyName(String friendlyName);

    Proxy.Builder proxy(SipURI to);

}
