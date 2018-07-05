package mpg.servers.akka.http

import akka.actor.{Actor, ActorSystem, Props, Timers}
import akka.cluster.Cluster
import akka.stream.ActorMaterializer
import mpg.Server.TerminationFunction
import mpg.{Server, ServerConfig}

import scala.concurrent.ExecutionContextExecutor

class HeartBeatActor extends Actor with WsUiSingletonSupport with Timers {

  val cluster = Cluster(context.system)

  override def receive: Receive = {
    case s: String =>
      println(s)
      wsUiProxy ! s
  }
}

object AkkaClusterNode extends Server {
  override def start(implicit config: ServerConfig): TerminationFunction = {

    val akkaContext = new AkkaContext(config.akkaSystemName.get, config.akkaClusterPort)
    implicit val system: ActorSystem = akkaContext.system
    implicit val materializer: ActorMaterializer = akkaContext.materializer
    implicit val executionContext: ExecutionContextExecutor = akkaContext.executionContext

    // TODO: control root of actor tree w/supervising actor

    EntityActor.startClusterSharding(system)
    WsUiSingletonSupport.initializeClusterSingleton(system)

    system.actorOf(Props[HeartBeatActor])
    system.actorOf(Props[ClusterListener])

    () => system.terminate()
  }
}
