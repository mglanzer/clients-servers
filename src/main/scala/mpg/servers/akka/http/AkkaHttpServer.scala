package mpg.servers.akka.http

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import mpg.Server.TerminationFunction
import mpg.{Server, ServerConfig}

import scala.concurrent.{ExecutionContextExecutor, Future}

object AkkaHttpServer extends Server {

  private var bindingFuture: Future[Http.ServerBinding] = _

  override def start(implicit config: ServerConfig): TerminationFunction = {

    val akkaContext = new AkkaContext(config.akkaSystemName.get, config.akkaClusterPort)
    implicit val system: ActorSystem = akkaContext.system
    implicit val materializer: ActorMaterializer = akkaContext.materializer
    implicit val executionContext: ExecutionContextExecutor = akkaContext.executionContext

    system.actorOf(Props(classOf[SimpleClusterListener]))

    def greeter: Flow[Message, Message, Any] =
      Flow[Message].mapConcat {
        case tm: TextMessage =>
          TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
        case bm: BinaryMessage =>
          // ignore binary messages but drain content to avoid the stream being clogged
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }

    val route: Route =
      pathSingleSlash {
        get {
          getFromResource("web/static/index.htm")
        }
      } ~
        path("ws") {
          handleWebSocketMessages(greeter)
        }

    bindingFuture = Http().bindAndHandle(route, config.interface, config.port.get)
    println(s"Server online at http://localhost:${config.port.get}/")

    () =>
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate())
  }
}
