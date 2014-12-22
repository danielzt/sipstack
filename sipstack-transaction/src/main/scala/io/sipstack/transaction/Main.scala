package io.sipstack.core

import akka.actor.{ActorSystem, Props}
import io.sipstack.transport.Connection
import io.sipstack.transport.Connection.{Write, Connect}

object Main {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem("sipstackio")
    // val connection = actorSystem.actorOf(Props[Connection], name = "whatever")
    val connection = actorSystem.actorOf(Connection.props("asdf"))
    connection ! Write(null)
    connection ! Connect("hello")
    connection ! Write(null)
  }

}