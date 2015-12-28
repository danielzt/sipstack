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
import io.sipstack.transport.FlowActor.IncomingRequest
import io.sipstack.transport.FlowActor.IncomingRequest
import io.sipstack.transport.FlowActor.OutgoingResponse
import io.sipstack.transport.FlowActor.IncomingRequest
import io.sipstack.transaction.SipTimers.TimerJ
import akka.actor.PoisonPill
import scala.collection.generic.IsTraversableLike

/**
 * @author jonas@jonasborjesson.com
 *
 */
object NonInviteServerTransaction {

  
}

/**
 * Implements the Non-INVITE Server Transaction as defined by
 * rfc3261 section 17.2.2.
 * 
 *<pre> 
 *                         |Request received
 *                         |pass to TU
 *                         V
 *                   +-----------+
 *                   |           |
 *                   | Trying    |-------------+
 *                   |           |             |
 *                   +-----------+             |200-699 from TU
 *                         |                   |send response
 *                         |1xx from TU        |
 *                         |send response      |
 *                         |                   |
 *      Request            V      1xx from TU  |
 *      send response+-----------+send response|
 *          +--------|           |--------+    |
 *          |        | Proceeding|        |    |
 *          +------->|           |<-------+    |
 *   +<--------------|           |             |
 *   |Trnsprt Err    +-----------+             |
 *   |Inform TU            |                   |
 *   |                     |                   |
 *   |                     |200-699 from TU    |
 *   |                     |send response      |
 *   |  Request            V                   |
 *   |  send response+-----------+             |
 *   |      +--------|           |             |
 *   |      |        | Completed |<------------+
 *   |      +------->|           |
 *   +<--------------|           |
 *   |Trnsprt Err    +-----------+
 *   |Inform TU            |
 *   |                     |Timer J fires
 *   |                     |-
 *   |                     |
 *   |                     V
 *   |               +-----------+
 *   |               |           |
 *   +-------------->| Terminated|
 *                   |           |
 *                   +-----------+
 * </pre> 
 */
class NonInviteServerTransaction(flow:ActorRef, next:ActorRef, config:TransactionLayerConfiguration, request:IncomingRequest) extends Actor {
  
  import scala.concurrent.duration._
  import context._
  
  
  /**
   * Whenever we receives a re-transmission we will re-send the last
   * response.
   */
  var lastResponse:SipResponse = null
  
  override def receive = init
  
  override def preStart {
  }
  
  def init:Receive = {
    case `request` => {
      become(trying)
      relay(request)
    }
    case _ => println("[Init] Strange, shouldn't happen or should it?")
  }
  
  def trying:Receive = {
    case msg:IncomingRequest => consumeRetransmission(msg)
    case resp:OutgoingResponse if resp.msg.isProvisional() => {
      lastResponse = resp.msg
      relay(resp)
      become(proceeding)
    }
    case resp:OutgoingResponse => {
      lastResponse = resp.msg
      relay(resp)
      toCompleted
    }
    case unknown => println("[Trying] Unkown event: " + unknown)
  }
  
  def proceeding:Receive = {
    case msg:IncomingRequest => processRetransmission(msg)
    case resp:OutgoingResponse if resp.msg.isProvisional() => {
      lastResponse = resp.msg
      relay(resp)
    }
    case resp:OutgoingResponse => {
      lastResponse = resp.msg
      relay(resp)
      toCompleted
    }
    case unknown => println("[Trying] Unkown event: " + unknown)
  }
  
  def completed:Receive = {
    case msg:IncomingRequest => processRetransmission(msg)
    case TimerJ(_) => terminate
    case unknown => println("[Completed] Unkown event: " + unknown)
  }
  
  def terminated:Receive = {
    case unknown => println("[Terminated] Unkown event: " + unknown)
  }
  
  private def toCompleted() {
    if (request.connectionId.isReliableTransport()) {
      self ! TimerJ(0) 
    } else {
      val timerJ = TimerJ(64 * 500)
      system.scheduler.scheduleOnce(timerJ.time millis, self, timerJ)
    }
    become(completed)
  }
  
  
  private def terminate {
    become(terminated)
    self ! PoisonPill
  }
  
  /**
   * If we receive a retransmission in the Trying state
   * we should simply just consume it.
   */
  private def consumeRetransmission(msg:IncomingRequest) {
    if (!isRetransmission(msg)) {
      // TODO: complain or something
    }
  }
  
  /**
   * if we receive a retransmisison while in the proceeding or completed state 
   * we should retransmit the last response.
   */
  private def processRetransmission(msg:IncomingRequest) {
      if (isRetransmission(msg)) {
          relay(lastResponse)
      } else {
        // TODO: do something about it
      }
  }
  
  private def relay(msg:IncomingRequest): Unit = {
    next ! msg
  }
  
  private def relay(response:OutgoingResponse) {
    flow ! response.msg
  }
  
  private def relay(response:SipResponse) {
      val now = System.currentTimeMillis()
      relay(new OutgoingResponse(now, request.connectionId, request.transactionId, request.callId, response))
  }
  
  /**
   * Check whether or not this is indeed a retransmission.
   */
  private def isRetransmission(msg:IncomingRequest) : Boolean = {
    true
  }
  
}