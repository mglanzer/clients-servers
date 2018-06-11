package mpg

import mpg.clients.akka.http.AkkaHttpClient
import mpg.servers.akka.http.{AkkaClusterNode, AkkaHttpServer, UiServer}
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
    val port: ScallopOption[Int] = opt[Int]("port", 'p')
    val clusterPort: ScallopOption[Int] = opt[Int]("clusterPort", 'c')
    val systemName: ScallopOption[String] = opt[String]("systemName", 'n')
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
      case "akka-http" => Some(AkkaHttpServer)
      case "akka-node" => Some(AkkaClusterNode)
      case "ui" => Some(UiServer)
      case _ => None
    }

    val config = ServerConfig(
      port = conf.serve.port.toOption,
      akkaSystemName = conf.serve.systemName.toOption,
      akkaClusterPort = conf.serve.clusterPort.toOption
    )

    resolveServer
      .map(server => {
        server.start(config)
      })
      .foreach(terminationFunc => {
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
