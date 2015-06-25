package io.sipstack.netty.codec.sip.tu;

/**
 * @author jonas@jonasborjesson.com
 */
public interface Proxy {

    void cancel();

    void terminate();

    void die();

    class Builder {

    }
}

