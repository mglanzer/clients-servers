package mpg

import mpg.Server.TerminationFunction

case class ServerConfig(
                         interface: String = "localhost",
                         port: Option[Int] = None,
                         akkaSystemName: Option[String] = None,
                         akkaClusterPort: Option[Int] = None,
                         eventListener: ServerEvent => Unit = _ => {}
                       )

object Server {
  type TerminationFunction = () => Unit
}

trait Server {
  def start(implicit config: ServerConfig): TerminationFunction
}

case class ServerEvent(source: String, event: String, value: String)
