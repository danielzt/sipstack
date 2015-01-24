package io.sipstack.transport

import scala.collection.mutable.Map
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import io.sipstack.netty.codec.sip.ConnectionId
import io.sipstack.netty.codec.sip.SipMessageEvent
import io.sipstack.transport.FlowActor.ReadEvent

object TransportSupervisor {
  
  /**
   * 
   */
  case class Connect()
  
  def props(nxt: ActorRef): Props = Props(new TransportSupervisor(nxt))
}

/**
 * The 
 *
 * Created by jonas@jonasborjesson.com
 */
class TransportSupervisor(next: ActorRef) extends Actor {
  import TransportSupervisor._
  
  def receive = {
    case event:SipMessageEvent => relayRead(event)
    case _ => println("Ignoring the request")
  }
  
  /**
   * For incoming SIP messages, we simply try and lookup
   * the corresponding [[Flow]]
   */
  def relayRead(event: SipMessageEvent) : Unit = {
    val id = event.getConnection.id

    val child = context.child(id.encodeAsString)
    if (child.isDefined) {
      child.get ! event
    } else {
      context.watch(context.actorOf(FlowActor.props(next, id, event.getConnection), id.encodeAsString)) ! event
    }
  }

}
