package mpg

import mpg.Server.TerminationFunction

case class ServerConfig(
                         interface: String = "localhost",
                         port: Int = 8088
                       )

object Server {
  type TerminationFunction = () => Unit
}

trait Server {
  def start(implicit config: ServerConfig): TerminationFunction
}
