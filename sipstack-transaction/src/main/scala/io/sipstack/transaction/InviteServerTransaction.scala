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
import io.sipstack.transport.FlowActor.OutgoingResponse
import akka.actor.PoisonPill

/**
 * @author jonas@jonasborjesson.com
 *
 */
object InviteServerTransaction {

  
}

/**
 * Implements the Invite Server Transaction as specified by
 * rfc3261 section
 * 
 * <pre>
 *                     |INVITE
 *                     |pass INV to TU
 *  INVITE             V send 100 if TU won't in 200ms
 *  send response+-----------+
 *      +--------|           |--------+101-199 from TU
 *      |        | Proceeding|        |send response
 *      +------->|           |<-------+
 *               |           |          Transport Err.
 *               |           |          Inform TU
 *               |           |--------------->+
 *               +-----------+                |
 *  300-699 from TU |     |2xx from TU        |
 *  send response   |     |send response      |
 *                  |     +------------------>+
 *                  |                         |
 *  INVITE          V          Timer G fires  |
 *  send response+-----------+ send response  |
 *      +--------|           |--------+       |
 *      |        | Completed |        |       |
 *      +------->|           |<-------+       |
 *               +-----------+                |
 *                  |     |                   |
 *              ACK |     |                   |
 *              -   |     +------------------>+
 *                  |        Timer H fires    |
 *                  V        or Transport Err.|
 *               +-----------+  Inform TU     |
 *               |           |                |
 *               | Confirmed |                |
 *               |           |                |
 *               +-----------+                |
 *                     |                      |
 *                     |Timer I fires         |
 *                     |-                     |
 *                     |                      |
 *                     V                      |
 *               +-----------+                |
 *               |           |                |
 *               | Terminated|<---------------+
 *               |           |
 *               +-----------+
 * 
 * </pre>
 */
class InviteServerTransaction(flow:ActorRef, next:ActorRef, config:TransactionLayerConfiguration, invite:IncomingRequest) extends Actor {
  
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
    system.scheduler.scheduleOnce(200 millis, self, "100trying")
    become(proceeding)
    relay(invite)
  }
  
  /**
   * Implements the proceeding state where we will
   * 1. retransmit the latest response if we receive a retransmitted INVITE
   * 2. any response received by the TU will be sent out.
   * 2.1. If reeponse is 101-199 -> stay in current state
   * 2.2 if response is 2xx -> transition to terminated state
   * 2.3 if response is error -> transition to completed state
   */
  def proceeding:Receive = {
    case event:IncomingRequest => println("received a request, better be a retransmitted invite " + event.msg)
    case event:OutgoingResponse => {
      relay(event)
      if (event.msg.isSuccess()) {
        terminate
      } else if (event.msg.isFinal()) {
        println("Received error response, transition to completed");
        // schedule timers etc
        become(completed)
      }
    }
    case "100trying" => {
        if (lastResponse == null) {
            val now = System.currentTimeMillis()
            val trying = invite.msg createResponse 100
            relay(new OutgoingResponse(now, invite.connectionId, invite.transactionId, invite.callId, trying))
        }
    }
    case unknown => println("Received some unknown shit while in proceeding" + unknown); 
  }
  
  private def terminate {
    become(terminated)
    self ! PoisonPill
  }
  
  def completed:Receive = {
    case event:IncomingRequest if event.msg.isAck => {
      become(confirmed)
    }
    
    case unknown => println("Received some unknown shit while in completed" + unknown); 
  }
  
  def terminated:Receive = {
    case unknown => println("ok, got something in the terminated state " + unknown)
  }
  
  def confirmed:Receive = {
    ???
  }
  
  private def relay(response:OutgoingResponse) {
    if (lastResponse == null || lastResponse.getStatus < response.msg.getStatus) {
      lastResponse = response.msg
    }
    flow ! response.msg
  }
  
  private def relay(msg:IncomingRequest): Unit = {
    next ! msg
  }
  
  
  
}