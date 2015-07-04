package io.sipstack.netty.codec.sip.application;

import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.sipstack.netty.codec.sip.Clock;
import io.sipstack.netty.codec.sip.MockChannelHandlerContext;
import io.sipstack.netty.codec.sip.SipStackTestBase;
import io.sipstack.netty.codec.sip.SystemClock;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * @author jonas@jonasborjesson.com
 */
public class ApplicationControllerTest extends SipStackTestBase {

    private ApplicationController applicationController;

    private Clock clock = new SystemClock();

    private MockApplicationInstanceCreator instanceCreator;

    private CountDownLatch requestLatch;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        requestLatch = new CountDownLatch(1);
        instanceCreator = new MockApplicationInstanceCreator(requestLatch);
        applicationController = new ApplicationController(clock, null, instanceCreator);
        defaultChannelCtx = new MockChannelHandlerContext(applicationController);
    }

    @Test(timeout = 500)
    public void testInvokeApplicationOnInvite() throws Exception {
        applicationController.channelRead(defaultChannelCtx, createEvent(defaultInviteRequest));
        requestLatch.await();

        // we should have proxied the request so ensure that there was
        // a proxied invite present.
        defaultChannelCtx.writeLatch().await();
    }

    public static class MockApplicationInstanceCreator implements ApplicationInstanceCreator {

        private final CountDownLatch requestLatch;

        public MockApplicationInstanceCreator(final CountDownLatch requestLatch) {
            this.requestLatch = requestLatch;
        }

        @Override
        public Buffer getId(final SipMessage message) {
            return message.getCallIDHeader().getCallId();
        }

        @Override
        public ApplicationInstance createInstance(final Buffer id, final SipMessage message) {
            return new MockApplicationInstance(id, requestLatch);
        }
    }

    public static class MockApplicationInstance extends ApplicationInstance {

        /**
         * Latch to use whenever we receive a new request.
         */
        private final CountDownLatch requestLatch;

        public MockApplicationInstance(final Buffer id, final CountDownLatch requestLatch) {
            super(id);
            this.requestLatch = requestLatch;
        }

        @Override
        public void onRequest(final SipRequest request) {
            requestLatch.countDown();
            proxy("sip:hello@whatever.com").start();
        }
    }
}