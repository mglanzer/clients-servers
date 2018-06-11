package mpg.servers.akka.http

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
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

    val route: Route =
      pathSingleSlash {
        get {
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Say hello to akka-http"))
        }
      }

    bindingFuture = Http().bindAndHandle(route, config.interface, config.port.get)
    println(s"Server online at http://localhost:${config.port.get}/")

    () =>
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate())
  }
}
