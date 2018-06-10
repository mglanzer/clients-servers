package mpg.servers.akka.http

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import mpg.Server.TerminationFunction
import mpg.{Server, ServerConfig}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{ExecutionContextExecutor, Future}

class AkkaContext(clusterPort: Int) {

  private val configOverrides =
    ConfigFactory.parseString(
      s"""
          akka.remote.netty.tcp.port=$clusterPort
          akka.remote.artery.canonical.port=$clusterPort
          """)
      .withFallback(ConfigFactory.load("application.conf"))

  private val config: Config = ConfigFactory.load(configOverrides)

  println(config.getAnyRef("akka.remote.netty.tcp.port"))
  println(config.getAnyRef("akka.cluster.seed-nodes"))

  lazy implicit val system: ActorSystem = ActorSystem("AkkaHttpServer", config)
  lazy implicit val materializer: ActorMaterializer = ActorMaterializer()
  lazy implicit val executionContext: ExecutionContextExecutor = system.dispatcher
}

class SimpleClusterListener extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) ⇒
      log.info("Member is Up: {}", member.address)
    case UnreachableMember(member) ⇒
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) ⇒
      log.info(
        "Member is Removed: {} after {}",
        member.address, previousStatus)
    case _: MemberEvent ⇒ // ignore
  }
}

object AkkaHttpServer extends Server {

  private var bindingFuture: Future[Http.ServerBinding] = _

  override def start(implicit config: ServerConfig): TerminationFunction = {

    val akkaContext = new AkkaContext(config.akkaClusterPort)
    implicit val system: ActorSystem = akkaContext.system
    implicit val materializer: ActorMaterializer = akkaContext.materializer
    implicit val executionContext: ExecutionContextExecutor = akkaContext.executionContext

    system.actorOf(Props(classOf[SimpleClusterListener]))

    val route: Route =
      pathSingleSlash {
        get {
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Say hello to akka-http"))
        }
      }

    bindingFuture = Http().bindAndHandle(route, config.interface, config.port)

    () =>
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate())
  }
}
