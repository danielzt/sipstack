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
    case _ => println("got something but not sure what: " + this)
  }
  
  def doInvite(event:IncomingRequest) {
    val invite = event.msg.toRequest
    val now = System.currentTimeMillis()
    val ringing = invite createResponse 180
    sender ! new OutgoingResponse(now, event.connectionId, event.transactionId, event.callId, ringing)
    // sender ! request.createResponse(180)
    // sender ! request.createResponse(200)
  }
  
  def doAck(request:SipRequest) {
    // ignore
  }
  
  def doBye(request:SipRequest) {
    sender ! request.createResponse(200)
    self ! PoisonPill
  }
  
}