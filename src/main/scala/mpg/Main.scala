package mpg

import mpg.servers.akka.http.AkkaHttpServer
import org.rogach.scallop._

import scala.io.StdIn

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val server: ScallopOption[String] = opt[String]("server", 's', required = true)
  val port: ScallopOption[Int] = opt[Int]("port", 'p', required = true)
  verify()
}

object Main {
  def main(args: Array[String]) {
    val conf = new Conf(args)

    def getServer: Option[Server] = conf.server().toLowerCase() match {
      case "akka" => Some(new AkkaHttpServer)
      case _ => None
    }

    getServer
      .foreach(server => {
        val msg = server.start(ServerConfig(conf.port()))
        println(msg)
        println("Press Enter to stop")
        StdIn.readLine()
      })
  }
}
