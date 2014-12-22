package io.sipstack.transport

import akka.actor.{Props, Stash, Actor}
import io.pkts.packet.sip.SipMessage
import io.sipstack.core.Bootstrap
import io.sipstack.transport.Connection.{Connect, Write}

object Connection {

  sealed trait State

  /**
   * The {@link Connection} is initially in the @{@link Closed} state
   *
   */
  case class Closed() extends State
  case class Closing() extends State

  case class Connecting() extends State

  /**
   * In the @{@link Active} state the @{@link Connection} is
   * able to send/receive data.
   */
  case class Active() extends State

  case class Write(msg: SipMessage)
  case class Connect(address: String)
  case class Disconnect()
  case class Connected()

  def props(id : String): Props = Props(new Connection(id))
}


/**
 * Created by jonas on 12/20/14.
 */
class Connection(id : String) extends Actor with Stash {
  import Connection._

  def receive = closed

  def closed:Receive = {
    case Connect(address) => {
      context.become(connecting)
    }

    case Write(msg) => println("not connected to stashing away the message"); stash()
    case _ => // drop
  }

  /**
   *
   * @return
   */
  def connecting:Receive = {
    case Write(msg) => println("Gee dude, i'm connecting! Relax"); stash()
    case _ => // drop
  }

  // def connecting:Receive = {
    // case
  // }

  def active:Receive = {
    case Write(msg) => println("yeah, i'm supposed to write a message " + msg)
  }



}
