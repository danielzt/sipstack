/**
 *
 */
package io.sipstack.application

import akka.actor.Actor
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.actorRef2Scala
import io.pkts.packet.sip.SipRequest
import io.sipstack.transport.FlowActor.IncomingRequest
import io.sipstack.transport.FlowActor.OutgoingResponse

object ApplicationActor {
  
  abstract class ConsistentHashing {
    def key():Object
  }
  
  
  def props(): Props = Props(new ApplicationActor());
  
}

/**
 * @author jonas@jonasborjesson.com
 */
final class ApplicationActor extends Actor {
  import ApplicationActor._
  
  override def preStart = {
    
  }
  
  def receive = {
    case msg:IncomingRequest if msg.msg.isInvite() => doInvite(msg)
    case msg:IncomingRequest if msg.msg.isAck() => doAck(msg)
    case msg:IncomingRequest if msg.msg.isBye() => doBye(msg)
    case unknown => println("[ApplicationActor] got something but not sure what: " + unknown)
  }
  
  def doInvite(event:IncomingRequest) {
    val invite = event.msg.toRequest
    val now = System.currentTimeMillis()
    val ringing = invite createResponse 180
    sender ! new OutgoingResponse(now, event.connectionId, event.transactionId, event.callId, ringing)
    val ok = invite createResponse 200
    sender ! new OutgoingResponse(now, event.connectionId, event.transactionId, event.callId, ok)
  }
  
  def doAck(event:IncomingRequest) {
    // ignore
  }
  
  def doBye(event:IncomingRequest) {
    val now = System.currentTimeMillis()
    val ok = event.msg.createResponse(200)
    sender ! new OutgoingResponse(now, event.connectionId, event.transactionId, event.callId, ok)
    self ! PoisonPill
  }
  
}