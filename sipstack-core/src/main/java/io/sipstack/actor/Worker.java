package io.sipstack.actor;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Worker implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(Worker.class);

    /**
     * Just a simple id of the worker. Mainly used for logging/debugging
     * purposes.
     */
    private final int id;

    private final BlockingQueue<Runnable> queue;

    // private final PipeLineFactory pipeFactory;

    /**
     * 
     */
    public Worker(final int id, final BlockingQueue<Runnable> queue) {
        this.id = id;
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                final Runnable event = this.queue.take();
                event.run();
            } catch (final Throwable t) {
                // do something cool
                t.printStackTrace();
            }
        }
    }


}
