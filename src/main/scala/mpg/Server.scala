package mpg

import mpg.Server.TerminationFunction

case class ServerConfig(
                         interface: String = "localhost",
                         port: Int = 8088,
                         akkaClusterPort: Int = 5221,
                         eventListener: ServerEvent => Unit = _ => {}
                       )

object Server {
  type TerminationFunction = () => Unit
}

trait Server {
  def start(implicit config: ServerConfig): TerminationFunction
}

case class ServerEvent(source: String, event: String, value: String)
