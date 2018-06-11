package mpg.servers.akka.http

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContextExecutor

object AkkaContext {
  private lazy val applicationConfig = ConfigFactory.load("application.conf")
}

class AkkaContext(systemName: String, clusterPort: Option[Int]) {

  import AkkaContext._

  private val configOverrides = clusterPort match {
    case Some(port) => ConfigFactory.parseString(
      s"""
          akka.remote.netty.tcp.port=$port
          akka.remote.artery.canonical.port=$port
          """)
    case _ => ConfigFactory.parseString(
      s"""
          akka.cluster.seed-nodes=[]
          """)
  }

  println(s"Configuring overrides\n$configOverrides")

  private val config: Config = configOverrides.withFallback(applicationConfig)

  lazy implicit val system: ActorSystem = ActorSystem(systemName, config)
  lazy implicit val materializer: ActorMaterializer = ActorMaterializer()
  lazy implicit val executionContext: ExecutionContextExecutor = system.dispatcher
}
