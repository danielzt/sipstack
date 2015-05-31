package io.sipstack.netty.codec.sip;

import io.pkts.packet.sip.SipMessage;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author jonas@jonasborjesson.com
 */
public class MockConnection implements Connection {

    private final ConnectionId id;

    /**
     * Every time a new message is sent via this connection this latch
     * will be counted down.
     */
    private final AtomicReference<CountDownLatch> latch;

    private List<SipMessage> messages = new CopyOnWriteArrayList<>();

    private AtomicInteger totalMessagesSent = new AtomicInteger(0);

    public MockConnection(final ConnectionId id) {
        this(id, new CountDownLatch(1));
    }

    public MockConnection(final ConnectionId id, final CountDownLatch latch) {
        this.id = id;
        this.latch = new AtomicReference<>(latch);
    }

    public CountDownLatch latch() {
        return latch.get();
    }

    /**
     * Check what the first element on the messages list is.
     *
     * @return
     */
    public SipMessage peek() {
        return messages.get(0);
    }

    public SipMessage consume() {
        return messages.remove(0);
    }

    /**
     * Check how many messages have been sent in total through this connection.
     *
     * Note, this is not the same as the current number of UN-PROCESSED messages that
     * is still on the internal message list.
     *
     * @return
     */
    public int count() {
        return totalMessagesSent.get();
    }

    public int countUnProcessedMessages() {
        return messages.size();
    }

    /**
     * Reset the internal latch to the new count. Note, you MUST NOT ever
     * hold onto the count down latch since we will create a new latch
     * when we are asked to reset it.
     *
     * @param count
     */
    public void resetLatch(final int count) {
        latch.set(new CountDownLatch(count));
    }

    @Override
    public ConnectionId id() {
        return id;
    }

    @Override
    public int getLocalPort() {
        return id.getLocalPort();
    }

    @Override
    public byte[] getRawLocalIpAddress() {
        return id.getRawLocalIpAddress();
    }

    @Override
    public String getLocalIpAddress() {
        return id.getLocalIpAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public int getRemotePort() {
        return id.getRemotePort();
    }

    @Override
    public byte[] getRawRemoteIpAddress() {
        return id.getRawRemoteIpAddress();
    }

    @Override
    public String getRemoteIpAddress() {
        return id.getRemoteIpAddress();
    }

    @Override
    public Transport getTransport() {
        return id.getProtocol();
    }

    @Override
    public boolean isUDP() {
        return id.isUDP();
    }

    @Override
    public boolean isTCP() {
        return id.isTCP();
    }

    @Override
    public boolean isTLS() {
        return id.isTLS();
    }

    @Override
    public boolean isSCTP() {
        return id.isSCTP();
    }

    @Override
    public boolean isWS() {
        return isWS();
    }

    @Override
    public void send(final SipMessage msg) {
        messages.add(msg);
        totalMessagesSent.addAndGet(1);
        latch.get().countDown();
    }

    @Override
    public boolean connect() {
        throw new RuntimeException("not implemented yet");
    }

}
