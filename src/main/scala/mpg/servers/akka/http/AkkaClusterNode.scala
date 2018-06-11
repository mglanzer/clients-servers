package mpg.servers.akka.http

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import mpg.Server.TerminationFunction
import mpg.{Server, ServerConfig}

import scala.concurrent.ExecutionContextExecutor

object AkkaClusterNode extends Server {
  override def start(implicit config: ServerConfig): TerminationFunction = {

    val akkaContext = new AkkaContext(config.akkaSystemName.get, config.akkaClusterPort)
    implicit val system: ActorSystem = akkaContext.system
    implicit val materializer: ActorMaterializer = akkaContext.materializer
    implicit val executionContext: ExecutionContextExecutor = akkaContext.executionContext

    // TODO: control root of actor tree w/supervising actor

    system.actorOf(Props(classOf[SimpleClusterListener]))

    () => system.terminate()
  }
}
