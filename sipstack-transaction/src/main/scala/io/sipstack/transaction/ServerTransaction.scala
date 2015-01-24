package io.sipstack.transaction

import akka.actor.ActorRef
import akka.actor.Props
import io.sipstack.transport.FlowActor.IncomingRequest
import io.sipstack.config.TransactionLayerConfiguration

final object ServerTransaction {
  
  def props(next:ActorRef, config:TransactionLayerConfiguration, req:IncomingRequest): Props = {
    
    if (req.msg.isInvite()) {
      Props(new InviteServerTransaction(next, config, req))
    } else {
        Props()
    }
  }
  
}
