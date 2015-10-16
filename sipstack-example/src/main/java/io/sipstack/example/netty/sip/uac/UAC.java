/**
 * 
 */
package io.sipstack.example.netty.sip.uac;

import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.header.FromHeader;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.example.netty.sip.SimpleSipStack;
import io.sipstack.netty.codec.sip.Connection;

/**
 * @author jonas@jonasborjesson.com
 */
public final class UAC {

    private final SimpleSipStack stack;

    // we will be using the same from for all requests
    private final FromHeader from = FromHeader.withHost("example.com").withUser("bob").build();

    /**
     * 
     */
    public UAC(final SimpleSipStack stack) {
        this.stack = stack;
    }

    public void send() throws Exception {
        final String host = "127.0.0.1";
        final int port = 5070;
        final Connection connection = this.stack.connect(host, port);
        this.from.setParameter(Buffers.wrap("tag"), FromHeader.generateTag());
        final ViaHeader via =
                ViaHeader.withHost(host).withPort(port).withBranch(ViaHeader.generateBranch()).withTransportTCP().build();
        final SipRequest invite = SipRequest.invite("sip:alice@example.com").withFromHeader(UAC.this.from).withTopMostViaHeader(via).build();
        connection.send(invite);
    }

    public static void main(final String[] args) throws Exception {
        final String ip = "127.0.0.1";
        final int port = 5060;
        final String transport = "udp";

        final UACHandler handler = new UACHandler();
        final SimpleSipStack stack = new SimpleSipStack(handler, ip, port);
        final UAC uac = new UAC(stack);
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    System.err.println("ok, sending");
                    uac.send();
                } catch (final Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }).start();
        stack.run();
    }

}
