package mpg.servers.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import mpg.Server.TerminationFunction
import mpg.{Server, ServerConfig}

import scala.concurrent.{ExecutionContextExecutor, Future}

object UiServer extends Server {

  private lazy implicit val system: ActorSystem = ActorSystem("UiServer")
  private lazy implicit val materializer: ActorMaterializer = ActorMaterializer()
  private lazy implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private var bindingFuture: Future[Http.ServerBinding] = _

  def greeter: Flow[Message, Message, Any] =
    Flow[Message].mapConcat {
      case tm: TextMessage =>
        TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
      case bm: BinaryMessage =>
        // ignore binary messages but drain content to avoid the stream being clogged
        bm.dataStream.runWith(Sink.ignore)
        Nil
    }

  val websocketRoute: Route =
    path("ws") {
      handleWebSocketMessages(greeter)
    }

  override def start(implicit config: ServerConfig): TerminationFunction = {

    bindingFuture = Http().bindAndHandle(websocketRoute, config.interface, config.port)

    () =>
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate())
  }
}
