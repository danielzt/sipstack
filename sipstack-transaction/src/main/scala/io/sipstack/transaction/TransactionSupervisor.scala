package io.sipstack.transaction

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import io.sipstack.config.TransactionLayerConfiguration
import io.sipstack.transport.FlowActor.IncomingRequest
import io.sipstack.transport.FlowActor.IncomingResponse
import akka.actor.Terminated

final object TransactionSupervisor {

  def props(next:ActorRef, config:TransactionLayerConfiguration) : Props = Props(new TransactionSupervisor(next, config))
}

final class TransactionSupervisor(next:ActorRef, config:TransactionLayerConfiguration) extends Actor {
  
  override def receive = {
    case req:IncomingRequest => processIncomingRequest(req)
    case resp:IncomingResponse => processIncomingResponse(resp)
    case Terminated(_) => // ignore
    case msg => println("Guess I should take care of it: " + msg)
  }
  
  private def processIncomingRequest(req:IncomingRequest) {
    val child = context.child(req.transactionId.id)
    if (child.isDefined) {
      child.get forward req
    } else {
      context.watch(context.actorOf(ServerTransaction.props(sender, next, config, req), req.transactionId.id)) forward req
    }
  }
  
  private def processIncomingResponse(resp:IncomingResponse) {
    
  }
  

}