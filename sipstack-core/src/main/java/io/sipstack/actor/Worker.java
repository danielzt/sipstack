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

    private final BlockingQueue<Event> queue;

    private final PipeLineFactory pipeFactory;

    /**
     * 
     */
    public Worker(final int id, final PipeLineFactory pipe, final BlockingQueue<Event> queue) {
        this.id = id;
        this.pipeFactory = pipe;
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                final Event event = this.queue.take();
                final PipeLine pipe = this.pipeFactory.newPipeLine();
                final ActorContext ctx = ActorContext.withPipeLine(pipe);
                ctx.fireUpstreamEvent(event);
            } catch (final Throwable t) {
                // do something cool
                t.printStackTrace();
            }
        }
    }


}
