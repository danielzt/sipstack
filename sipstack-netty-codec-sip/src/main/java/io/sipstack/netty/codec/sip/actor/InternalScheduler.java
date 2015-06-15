package io.sipstack.netty.codec.sip.actor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.sipstack.netty.codec.sip.event.Event;

import java.time.Duration;

/**
 * @author jonas@jonasborjesson.com
 */
public interface InternalScheduler {

    // Cancellable schedule(Runnable job, Duration delay);

    /**
     * Schedule to deliver the particular event to the context after a certain delay.
     *
     * @param ctx the context to which the event will be delivered.
     * @param event the event to deliver
     * @param delay the delay before delivering the event.
     * @return a cancellable representing the task
     */
    Cancellable schedule(ChannelHandlerContext ctx, Event event, Duration delay);

    Cancellable schedule(ChannelInboundHandler layer, ChannelHandlerContext ctx, Event event, Duration delay);
}
