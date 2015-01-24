/**
 * 
 */
package io.sipstack.application

import akka.actor.Actor
import akka.actor.Props
import io.sipstack.netty.codec.sip.SipMessageEvent
import akka.actor.ActorRef
import akka.actor.Terminated
import io.sipstack.transport.FlowActor.IncomingMessage
import io.sipstack.transport.FlowActor.Message

object ApplicationSupervisor {

  def props(): Props = Props(new ApplicationSupervisor())
}

final class ApplicationSupervisor extends Actor {
  
  override def preStart() {
    println("Starting a new ApplicationSupservisor: " + this)
  }
  
  def receive = {
    case Terminated(actor) => // ignore for now
    case msg:IncomingMessage => dispatch(msg)
    case que => println("unknown event, ignoring. Got " + que.getClass())
  }
  
  private def dispatch(event:IncomingMessage): Unit = {
    val callId = event.callId
    val child = context.child(callId)
    if (child.isDefined) {
      child.get forward event
    } else {
      context.watch(context.actorOf(ApplicationActor.props(), callId)) forward event
    }
  }
  
}