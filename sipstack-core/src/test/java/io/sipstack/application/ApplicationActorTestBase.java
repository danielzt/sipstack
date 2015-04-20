package io.sipstack.application;

import io.sipstack.actor.SipTestBase;
import org.junit.After;
import org.junit.Before;

/**
 * @author jonas@jonasborjesson.com
 */
public class ApplicationActorTestBase extends SipTestBase {

    public ApplicationActorTestBase() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}