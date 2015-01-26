package io.sipstack.transaction

import akka.actor.ActorRef
import akka.actor.Props
import io.sipstack.config.TransactionLayerConfiguration
import io.sipstack.transport.FlowActor.IncomingRequest

final object ServerTransaction {
  

  
  def props(previous:ActorRef, next:ActorRef, config:TransactionLayerConfiguration, req:IncomingRequest): Props = {
    if (req.msg.isInvite()) {
      Props(new InviteServerTransaction(previous, next, config, req))
    } else {
      Props(new NonInviteServerTransaction(previous, next, config, req))
    }
  }
  
}
