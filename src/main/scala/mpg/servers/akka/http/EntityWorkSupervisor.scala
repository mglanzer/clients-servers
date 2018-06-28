package mpg.servers.akka.http

import akka.actor.{Actor, ActorLogging, ActorRef, Timers}
import mpg.servers.akka.http.EntityWorkSupervisor.HaveWork

import scala.concurrent.duration._

object EntityWorkSupervisor {

  case object HaveWork

}

class EntityWorkSupervisor(shardRef: ActorRef) extends Actor with ActorLogging with Timers {

  private val entityIds = Iterator.continually(Seq.range(1L, 20L)).flatten
  var workCounter: Long = 0

  timers.startPeriodicTimer("distributeWork", HaveWork, 1.seconds)

  override def receive: Receive = {
    case HaveWork =>
      val entityId = entityIds.next()
      val work = workCounter
      workCounter += 1
      log.info(s"Distributing work $work to $entityId")
      shardRef ! EntityWork(entityId, work)
  }
}
