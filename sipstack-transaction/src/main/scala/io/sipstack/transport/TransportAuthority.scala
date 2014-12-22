package io.sipstack.transport

import akka.actor.Actor

/**
 * The {@link TransportAuthority} is responsible for maintaining all {@link Conn}
 *
 * Created by jonas@jonasborjesson.com
 */
class TransportAuthority extends Actor {

  def receive = {
    case 'a' => println("hello")
  }

}
