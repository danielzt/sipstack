/**
 *
 */
package io.sipstack.transaction

import akka.actor.Actor
import akka.actor.Props

/**
 * @author jonas
 *
 */
class HelloWorld extends Actor {
  
  override def preStart(): Unit = {
    val greeter = context.actorOf(Props[Greeter], "greeter")
    greeter ! Greeter.Greet
  }
  
  def receive = {
    case Greeter.Done => context.stop(self)
  }

  def apa = {
  }
  
}