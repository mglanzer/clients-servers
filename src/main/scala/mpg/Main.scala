package mpg

import mpg.clients.akka.http.AkkaHttpClient
import mpg.servers.akka.http.AkkaHttpServer
import org.rogach.scallop._

import scala.io.StdIn

object Commands {
  val serve = "serve"
  val request = "request"
}

//noinspection TypeAnnotation
class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {

  val serve = new Subcommand(Commands.serve) {
    val server: ScallopOption[String] = opt[String]("server", 's', required = true)
    val port: ScallopOption[Int] = opt[Int]("port", 'p', required = true)
  }

  val request = new Subcommand(Commands.request) {
    val client: ScallopOption[String] = opt[String]("client", 'c', required = true)
    val baseUrl: ScallopOption[String] = opt[String]("baseUrl", 'u', required = true)
  }

  addSubcommand(serve)
  addSubcommand(request)

  verify()
}

object Main {
  def main(args: Array[String]) {
    val conf = new Conf(args)
    args.head match {
      case StartServer(go) => go(conf)
      case StartClient(go) => go(conf)
    }
  }
}

object StartServer {
  def unapply(arg: String): Option[Conf => Unit] = {
    arg match {
      case Commands.serve => Some(go)
      case _ => None
    }
  }

  def go(conf: Conf): Unit = {
    def resolveServer: Option[Server] = conf.serve.server().toLowerCase() match {
      case "akka" => Some(new AkkaHttpServer)
      case _ => None
    }

    resolveServer
      .map(server => server.start(ServerConfig(port = conf.serve.port())))
      .foreach(terminationFunc => {
        println(s"Server online at http://localhost:${conf.serve.port()}/")
        println("Press Enter to stop")
        StdIn.readLine()
        terminationFunc()
      })
  }
}

object StartClient {
  def unapply(arg: String): Option[Conf => Unit] = {
    arg match {
      case Commands.request => Some(go)
      case _ => None
    }
  }

  def go(conf: Conf): Unit = {
    AkkaHttpClient.testRequest
  }
}
