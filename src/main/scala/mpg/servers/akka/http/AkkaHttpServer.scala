package mpg.servers.akka.http

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import mpg.Server.TerminationFunction
import mpg.servers.akka.http.ClusterListener.AddListener
import mpg.servers.akka.http.WSActor.{Connect, InMsg, OutMsg}
import mpg.{Server, ServerConfig}

import scala.concurrent.{ExecutionContextExecutor, Future}

object WSActor {

  case class Connect(outChannel: ActorRef)

  case class InMsg(value: String)

  case class OutMsg(value: String)

}

class WSActor() extends Actor {

  override def receive: Receive = {
    case Connect(outChannel) => context.become(connected(outChannel))
  }

  def connected(outChannel: ActorRef): Receive = {
    case InMsg(msg) => println(msg)
    case msg: OutMsg => outChannel ! msg
    case msg: String => outChannel ! OutMsg(msg)
  }
}

object AkkaHttpServer extends Server {

  private var bindingFuture: Future[Http.ServerBinding] = _

  override def start(implicit config: ServerConfig): TerminationFunction = {

    val akkaContext = new AkkaContext(config.akkaSystemName.get, config.akkaClusterPort)
    implicit val system: ActorSystem = akkaContext.system
    implicit val materializer: ActorMaterializer = akkaContext.materializer
    implicit val executionContext: ExecutionContextExecutor = akkaContext.executionContext

    val shardRef = EntityActor.startClusterSharding(system)
    system.actorOf(Props(classOf[EntityWorkSupervisor], shardRef))

    val clusterListener = system.actorOf(Props(classOf[ClusterListener]), "ClusterListener")

    var connectionCount = 0

    def wsConnection(): Flow[Message, Message, NotUsed] = {

      val wsActor = system.actorOf(Props[WSActor], "wsUi")
      connectionCount += 1
      clusterListener ! AddListener(wsActor)

      // In channel sends to wsActor
      val incomingMessages: Sink[Message, NotUsed] =
        Flow[Message].map {
          case TextMessage.Strict(text) => WSActor.InMsg(text)
          case _ => throw new RuntimeException("Only TextMessage is implemented")
        }.to(Sink.actorRef[WSActor.InMsg](wsActor, PoisonPill))

      // Out channel is provided to wsActor via connect
      val outgoingMessages: Source[Message, NotUsed] =
        Source.actorRef[WSActor.OutMsg](10, OverflowStrategy.fail)
          .mapMaterializedValue { outActor =>
            wsActor ! WSActor.Connect(outActor)
            NotUsed
          }.map(
          (outMsg: WSActor.OutMsg) => TextMessage(outMsg.value))

      Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
    }

    val route: Route =
      pathSingleSlash {
        get {
          getFromResource("web/static/index.htm")
        }
      } ~
        path("ws") {
          handleWebSocketMessages(wsConnection())
        }

    bindingFuture = Http().bindAndHandle(route, config.interface, config.port.get)
    println(s"Server online at http://localhost:${config.port.get}/")

    () =>
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate())
  }
}
