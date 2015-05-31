package io.sipstack.netty.codec.sip.transaction;

import io.sipstack.netty.codec.sip.actor.ActorContext;

/**
 * @author jonas@jonasborjesson.com
 */
public class NonInviteServerTransaction implements Transaction {

    @Override
    public void onReceive(ActorContext ctx, Object msg) {
        System.err.println("NonIniviteServerTransaction: Sure, now what...");
    }

}
