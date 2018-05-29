package mpg.servers.akka.http

import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.testkit.{ScalatestRouteTest, _}
import akka.util.ByteString
import mpg.servers.akka.http.UiServer._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class UiServerTest extends WordSpec with Matchers with ScalatestRouteTest {

  // create a testing probe representing the client-side
  val wsClient = WSProbe()

  "The ws endpoint" should {

    "Upgrade the connection and to stuff" in {

      // WS creates a WebSocket request for testing
      WS("/ws", wsClient.flow) ~> websocketRoute ~>
        check {
          // check response for WS Upgrade headers
          isWebSocketUpgrade shouldEqual true

          // manually run a WS conversation
          wsClient.sendMessage("Peter")
          wsClient.expectMessage("Hello Peter!")

          wsClient.sendMessage(BinaryMessage(ByteString("abcdef")))
          wsClient.expectNoMessage(100 millis)

          wsClient.sendMessage("John")
          wsClient.expectMessage("Hello John!")

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }
  }
}
