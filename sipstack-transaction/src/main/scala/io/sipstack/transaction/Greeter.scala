package io.sipstack.transaction


import akka.actor.Actor
import akka.actor.Actor.Receive

object Greeter {
  case object Greet
  case object Done
}

class Apa extends Actor {
  import context._
  def receive = {
    case Greeter.Greet =>
      println("hello")
      become(apa)

  }

  def apa:Receive = {
    case Greeter.Greet =>
      println("fup")
  }
}


class Greeter extends Actor {
  def receive = {
    case Greeter.Greet =>
      println("hello w  and queue no???")
      sender ! Greeter.Done
  }
}