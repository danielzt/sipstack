/**
 * 
 */
package io.sipstack.actor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jonas
 *
 */
public class PipeLineTest {

    private Actor one;
    private Actor two;
    private Actor three;
    private Actor four;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        this.one = mock(Actor.class);
        this.two = mock(Actor.class);
        this.three = mock(Actor.class);
        this.four = mock(Actor.class);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {}

    /**
     * Test basic operation of just simply moving forward in the pipe.
     */
    @Test
    public void testProgress() {
        final PipeLine pipe = PipeLine.withChain(this.one, this.two, this.three);
        assertThat(pipe.next().isPresent(), is(true));
        assertThat(pipe.next().get(), is(this.one));

        // since pipelines are immutable, when progressing
        // a new pipe will be returned which means that
        // when calling next on the original pipe
        // we are still at the same spot
        final PipeLine pipe2 = pipe.progress();
        assertThat(pipe2.next().get(), is(this.two));
        assertThat(pipe.next().get(), is(this.one));

        // again, we are calling progress on the original
        // pipe so wer are back to where we started
        final PipeLine pipe2b = pipe.progress();
        assertThat(pipe2b.next().get(), is(this.two));

        // last element
        final PipeLine theEnd = pipe2b.progress();
        assertThat(theEnd.next().get(), is(this.three));

        final PipeLine emptyPipe = theEnd.progress();
        assertThat(emptyPipe.next().isPresent(), is(false));
    }

    /**
     * Make sure that we can reverse the pipe when we are at the very beginning. This means that we
     * should still be pointing to 'one' and if we do progress we will walk off the pipe and nothing
     * should be returned on next. If we reverse again, the current element is still nothing but
     * then we can walk back to 'one', 'two' etc...
     * 
     */
    @Test
    public void testReversePipe001() {
        final PipeLine reverse = PipeLine.withChain(this.one, this.two, this.three, this.four).reverse();
        assertThat(reverse.next().get(), is(this.one));

        final PipeLine walkedOff = reverse.progress();
        assertThat(walkedOff.next().isPresent(), is(false));

        // still pointing to empty after reverse
        final PipeLine walkedOffReverse = walkedOff.reverse();
        assertThat(walkedOffReverse.next().isPresent(), is(false));

        // but if we now take a step ahead we are back to 'one'
        // and can walk all the way down again
        final PipeLine pipe1 = walkedOffReverse.progress();
        assertWalkAbout(pipe1, this.one, this.two, this.three, this.four);
    }

    /**
     * Make sure that if we walk half way down the pipe and then reverse that we get back the same
     * way we came
     */
    @Test
    public void testReversePipe002() {
        final PipeLine pipe = PipeLine.withChain(this.one, this.two, this.three, this.four);
        final PipeLine reversed = assertWalkAbout(pipe, this.one, this.two).reverse();

        // because of how assertWalkAbout works we should have progressed to three
        // and then we issued a reverse so we really should be able to walk from
        // three back down to one
        final PipeLine empty = assertWalkAbout(reversed, this.three, this.two, this.one);

        // and, again, because of how assertWalkAbout operates we should now be standing
        // outside the last element and therefore this pipe should be empty
        assertThat(empty.next().isPresent(), is(false));
    }

    /**
     * Just make sure that if we walk all the way and then reverse, that we dont get stuck in some
     * one off indexing...
     */
    @Test
    public void testReversePipe003() {
        final PipeLine pipe = PipeLine.withChain(this.one, this.two, this.three, this.four);
        final PipeLine reversed = assertWalkAbout(pipe, this.one, this.two, this.three, this.four).reverse();
        assertThat(reversed.next().isPresent(), is(false));
        // progress to consume the empty and then we should have 4, 3, 2, 1
        assertWalkAbout(reversed.progress(), this.four, this.three, this.two, this.one);
    }

    public PipeLine assertWalkAbout(final PipeLine original, final Actor... actors) {
        PipeLine pipe = original;
        for (final Actor actor : actors) {
            assertThat(pipe.next().get(), is(actor));
            pipe = pipe.progress();
        }
        return pipe;
    }

}
