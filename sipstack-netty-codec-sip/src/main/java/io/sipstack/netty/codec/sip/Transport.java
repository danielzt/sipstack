package io.sipstack.netty.codec.sip;

import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;

/**
 * Note, enums should be all caps but SIP is annoying and for transports in a SipURI the transport
 * is supposed to be lower case so therefore we just made these into lower case as well. Just easier
 * that way. 
 * 
 * Created by jonas@jonasborjesson.com
 */
public enum Transport {
    udp(Buffers.wrap("udp")),
    tcp(Buffers.wrap("tcp")),
    tls(Buffers.wrap("tls")),
    sctp(Buffers.wrap("sctp")),
    ws(Buffers.wrap("ws")),
    wss(Buffers.wrap("wss"));

    final Buffer buffer;
    final Buffer upperCaseBuffer;

    Transport(final Buffer buffer) {
        this.buffer = buffer;
        this.upperCaseBuffer = Buffers.wrap(buffer.toString().toUpperCase());
    }

    public Buffer toBuffer() {
        return buffer;
    }

    public Buffer toUpperCaseBuffer() {
        return upperCaseBuffer;
    }
}
