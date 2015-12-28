package io.sipstack.transport

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Stash
import akka.actor.actorRef2Scala
import io.pkts.packet.sip.SipMessage
import io.pkts.packet.sip.SipRequest
import io.pkts.packet.sip.SipResponse
import io.sipstack.netty.codec.sip.Connection
import io.sipstack.netty.codec.sip.ConnectionId
import io.sipstack.netty.codec.sip.SipMessageEvent
import io.sipstack.transaction.TransactionId

object FlowActor {
  
  sealed trait Message {
    def callId:String
    def msg:SipMessage
    def isRequest:Boolean = msg.isRequest()
  }
  
  sealed trait IncomingMessage extends Message {
    def arrivalTime:Long
    def connectionId:ConnectionId
    def transactionId:TransactionId
  }
  
  sealed trait OutgoingMessage extends Message {
  }
  
  case class IncomingRequest(arrivalTime:Long, connectionId:ConnectionId, transactionId:TransactionId, callId:String, msg:SipRequest) extends IncomingMessage
  case class IncomingResponse(arrivalTime:Long, connectionId:ConnectionId, transactionId:TransactionId, callId:String, msg:SipResponse) extends IncomingMessage
  
  case class OutgoingRequest(creationTime:Long, connectionId:ConnectionId, transactionId:TransactionId, callId:String, msg:SipRequest) extends OutgoingMessage
  case class OutgoingResponse(creationTime:Long, connectionId:ConnectionId, transactionId:TransactionId, callId:String, msg:SipResponse) extends OutgoingMessage

  case class WriteEvent(msg: SipMessage)
  case class ReadEvent(event: SipMessageEvent)
  case class Connect(address: String)
  case class Disconnect()
  case class Connected()
  case class Initialize()

  def props(next: ActorRef, id : ConnectionId, connection: Connection): Props = Props(new FlowActor(next, id, connection))
}


/**
 * A Flow represents a peer-to-peer relationship between two ip:port pairs that
 * is active for some time. In the case of a connection oriented transport, such
 * as tcp, a flow is the same as a tcp connection but a flow is also representing
 * connectionless transports such as udp.
 * 
 * Created by jonas on 12/20/14.
 */
class FlowActor(next: ActorRef, id : ConnectionId, connection: Connection) extends Actor with Stash {
  import FlowActor._
  
  override def preStart: Unit = {
    // self ! Initialize
    context.become(active)
  }
  
  def receive = init
  
  /**
   * Not sure we need this for now but just wanted to test some.
   */
  def init:Receive = {
    case Initialize => {
        println("Initializing myself") 
        context.become(active)
        unstashAll
    }
    case _ => stash
  }

  def closed:Receive = {
    case Connect(address) => {
      context.become(connecting)
    }

    case WriteEvent(msg) => println("not connected to stashing away the message"); stash()
    case _ => println("Yeah, I got something!!! " + this + " ConnectionId: " + id)// drop
  }

  /**
   *
   * @return
   */
  def connecting:Receive = {
    case WriteEvent(msg) => println("Gee dude, i'm connecting! Relax"); stash()
    case response:SipResponse => println("yeah, got a response")
    case _ => println("Strange, got something that didnt match");
  }

  /**
   * When we are active, we accept read and write events.
   */
  def active:Receive = {
    // case WriteEvent(msg) => println("ERROR: not handling write events right now...")
    // case event:SipMessageEvent if event.getMessage.isRequest => next ! Request(TransactionId.create(event.getMessage), event.getMessage.getCallIDHeader.getValue.toString, event.getMessage.toRequest)
    // case event:SipMessageEvent if event.getMessage.isResponse => next ! Response(TransactionId.create(event.getMessage), event.getMessage.getCallIDHeader.getValue.toString, event.getMessage.toResponse)
    case event:SipMessageEvent => processIncomingMessage(event)
    case response:SipResponse => connection.send(response)
    case _ => println("Strange, got something that didnt match");
  }
  
  def processIncomingMessage(event:SipMessageEvent) {
    val ts = event.getArrivalTime
    val sipMsg = event.getMessage
    val transactionId = TransactionId.create(sipMsg)
    val callId = sipMsg.getCallIDHeader.getValue.toString
    
    
    val msg = if (sipMsg.isRequest())  {
        IncomingRequest(ts, id, transactionId, callId, sipMsg.toRequest())
    } else {
        IncomingResponse(ts, id, transactionId, callId, sipMsg.toResponse())
    }
    
    next ! msg
    
    // event.getMessage.isRequest => next ! Request(TransactionId.create(event.getMessage), event.getMessage.getCallIDHeader.getValue.toString, event.getMessage.toRequest)
    
  }

}
