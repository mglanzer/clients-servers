package mpg

import scala.util.Try

case class ServerConfig(port: Int)

trait Server {
  def start(implicit config: ServerConfig): Try[String]
  def stop(): Unit
}
