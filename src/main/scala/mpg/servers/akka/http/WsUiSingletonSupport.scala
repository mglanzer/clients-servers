package mpg.servers.akka.http

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}

trait WsUiSingletonSupport {
  this: Actor =>

  lazy val wsUiProxy: ActorRef = WsUiSingletonSupport.wsUiProxy
}

object WsUiSingletonSupport {

  private var clusterSystem: Option[ActorSystem] = None

  def initializeClusterSingleton(system: ActorSystem): Unit = {
    clusterSystem = Some(system)

    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props[WSActor],
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system)),
      name = "wsUi")
  }

  lazy val wsUiProxy: ActorRef = createProxyActor

  private def createProxyActor = {
    clusterSystem
      .map(context => {
        context.actorOf(
          ClusterSingletonProxy.props(
            singletonManagerPath = "/user/wsUi",
            settings = ClusterSingletonProxySettings(context)),
          name = "wsUiProxy")
      })
      .getOrElse(throw new RuntimeException("Call initializeClusterSingleton before createProxyActor"))
  }
}
