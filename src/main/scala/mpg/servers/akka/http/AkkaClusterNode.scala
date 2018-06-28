package mpg.servers.akka.http

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import mpg.Server.TerminationFunction
import mpg.{Server, ServerConfig}
import akka.actor.Timers
import akka.cluster.Cluster

import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor

class HeartBeatActor extends Actor with Timers {

  val cluster = Cluster(context.system)

  timers.startPeriodicTimer("heartbeat", "heartbeat_" + cluster.selfAddress, 5.seconds)

  override def receive: Receive = {
    case s: String =>
      println(s)
      context.actorSelection("akka.tcp://" + context.system.name + "@localhost:2551/user/wsUi") ! s
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

    system.actorOf(Props[HeartBeatActor])
    system.actorOf(Props[ClusterListener])

    () => system.terminate()
  }
}
