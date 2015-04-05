package io.sipstack.actor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Super simple worker class that all it does is grab {@link Runnable}s off of a queue and executes
 * them. Each worker is always executing on the same single thread.
 * 
 * @author jonas@jonasborjesson.com
 *
 */
public class Worker implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(Worker.class);

    /**
     * Just a simple id of the worker. Mainly used for logging/debugging
     * purposes.
     */
    private final int id;

    private final BlockingQueue<Runnable> queue;

    /**
     * 
     */
    public Worker(final int id, final BlockingQueue<Runnable> queue) {
        this.id = id;
        this.queue = queue;
    }

    @Override
    public void run() {
        final List<Runnable> jobs = new ArrayList<>(10);
        while (true) {
            try {

                // use take as
                final Runnable event = queue.take();
                event.run();

                final int noOfJobs = this.queue.drainTo(jobs, 10);
                for (int i = 0; i < noOfJobs; ++i) {
                    final Runnable job = jobs.get(i);
                    job.run();
                }
                jobs.clear();
            } catch (final Throwable t) {
                // do something cool
                t.printStackTrace();
            }
        }
    }


}
