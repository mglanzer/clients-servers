package mpg.servers.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.DefaultConfigLoadingStrategy
import mpg.{Server, ServerConfig}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try

class AkkaHttpServer extends Server {

  private lazy implicit val system: ActorSystem = ActorSystem("AkkaHttpServer")
  private lazy implicit val materializer: ActorMaterializer = ActorMaterializer()
  private lazy implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private var bindingFuture: Future[Http.ServerBinding] = _

  override def start(implicit config: ServerConfig): Try[String] = {

    val route: Route =
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Say hello to akka-http"))
        }
      }

    Try({
      bindingFuture = Http().bindAndHandle(route, "localhost", config.port)
      s"Server online at http://localhost:${config.port}/"
    })
  }

  override def stop(): Unit = {
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
