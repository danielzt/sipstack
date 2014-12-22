/**
 * 
 */
package io.sipstack.core;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import io.sipstack.annotations.BYE;
import io.sipstack.annotations.INVITE;
import io.sipstack.netty.codec.sip.SipMessageEvent;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jonas@jonasborjesson.com
 */
public class DynamicApplicationInvokerTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {}

    @Test(timeout = 1000)
    public void testProcessAnnotations() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        final AnnotatedClass app = new AnnotatedClass(latch);

        final DynamicApplicationInvoker dynApp = DynamicApplicationInvoker.wrap(app);
        dynApp.doInvite(mock(SipMessageEvent.class));
        dynApp.doBye(mock(SipMessageEvent.class));

        latch.await();
    }

    /**
     * Make sure that we detect bad annotated classes.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessBadAnnotations() throws Exception {
        assertBadAnnotation(new BadAnnotedInvite());
        assertBadAnnotation(new BadAnnotedInvite2());
        assertBadAnnotation(new BadAnnotedInvite3());
        assertBadAnnotation(new NoAnnotedAnything());
    }

    private void assertBadAnnotation(final Object object) {
        try {
            DynamicApplicationInvoker.wrap(object);
            fail("Expected an IllegalArgumentException because the annotations are bad in the class");
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    // No annotations
    public static class NoAnnotedAnything { }

    public static class BadAnnotedInvite {
        @INVITE
        public void wrongSignature(final String hello) {}
    }

    public static class BadAnnotedInvite2 {
        @INVITE
        public void wrongSignature() {}
    }

    public static class BadAnnotedInvite3 {
        @INVITE
        public void wrongSignature(final SipMessageEvent event, final String tooManyArgs) {}
    }


    public static class AnnotatedClass {

        private final CountDownLatch latch;

        public AnnotatedClass(final CountDownLatch latch) {
            this.latch = latch;
        }

        @INVITE
        public void doInvite(final SipMessageEvent event) {
            this.latch.countDown();
        }

        @BYE
        public void theNameDoesntMatter(final SipMessageEvent event) {
            this.latch.countDown();
        }

    }

}
