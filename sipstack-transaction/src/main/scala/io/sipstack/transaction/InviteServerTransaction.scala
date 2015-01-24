/**
 *
 */
package io.sipstack.transaction

import akka.actor.Actor
import io.sipstack.transport.FlowActor.IncomingRequest
import io.pkts.packet.sip.SipResponse
import akka.actor.ActorRef
import io.sipstack.transport.FlowActor.IncomingRequest
import io.sipstack.config.TransactionLayerConfiguration
import io.sipstack.transport.FlowActor.OutgoingResponse

/**
 * @author jonas@jonasborjesson.com
 *
 */
object InviteServerTransaction {

  
}

class InviteServerTransaction(next:ActorRef, config:TransactionLayerConfiguration, invite:IncomingRequest) extends Actor {
  
  import scala.concurrent.duration._
  import context._
  
  
  /**
   * Whenever we receives a re-transmission we will re-send the last
   * response.
   */
  var lastResponse:SipResponse = null
  
  override def receive = init
  
  override def preStart {
    // val resp100Trying = req.request.createResponse(100);
    // system.scheduler.scheduleOnce(200 millis, self, resp100Trying)
  }
  
  def init:Receive = {
    case `invite` => processInitialInvite
    case _ => println("Strange, shouldn't happen or should it?")
  }
  
  /**
   * The very first message MUST be the same one that this [[InviteServerTransaction]]
   * was constructed with and once it has been received, we will do some initialization
   * and then transition to the proceeding state.
   */
  private def processInitialInvite:Unit = {
    sender ! invite.msg.createResponse(100)
    become(proceeding)
    relay(invite)
  }
  
  def proceeding:Receive = {
    case msg:OutgoingResponse => println("Yep, received some shit hwile in the proceeding state: " + msg); // sender ! req.msg.createResponse(100)
    case unknown => println("Received some unknown shit while in proceeding" + unknown); 
  }
  
  def completed:Receive = {
    ???
  }
  
  def confirmed:Receive = {
    ???
  }
  
  private def relay(msg:IncomingRequest): Unit = {
    next ! msg
  }
  
}