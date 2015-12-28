package io.sipstack.transport

import akka.actor.Actor
import akka.actor.Actor.Receive

/**
 * The Sender is responsible for taking a SIP message, resolving the next hop
 * and then finally find a Connection that it can use to actually send the
 * message. The Sender may have to resolve transport, port and IP-address
 * and will do so by talking to the DNS resolver.
 *
 * A Sender is responsible for sending exactly one message.
 *
 * Created by jonas@jonasborjesson.com
 */
class Sender extends Actor {

  def receive = {
    case "s" =>  println("received something")
  }

}
