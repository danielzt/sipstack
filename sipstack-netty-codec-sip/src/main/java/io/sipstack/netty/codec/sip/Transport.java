package io.sipstack.netty.codec.sip;

/**
 * Note, enums should be all caps but SIP is annoying and for transports in a SipURI the transport
 * is supposed to be lower case so therefore we just made these into lower case as well. Just easier
 * that way. 
 * 
 * Created by jonas@jonasborjesson.com
 */
public enum Transport {
    udp, tcp, tls, sctp, ws, wss;
}
