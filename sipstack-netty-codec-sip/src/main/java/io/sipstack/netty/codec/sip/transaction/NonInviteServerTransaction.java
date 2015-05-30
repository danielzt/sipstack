package io.sipstack.netty.codec.sip.transaction;

import io.sipstack.netty.codec.sip.actor.ActorContext;
import io.sipstack.netty.codec.sip.event.Event;

/**
 * @author jonas@jonasborjesson.com
 */
public class NonInviteServerTransaction implements Transaction {

    @Override
    public void onEvent(ActorContext ctx, Event event) {
        System.err.println("Sure, now what...");
    }

}
