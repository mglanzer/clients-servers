package mpg.servers.akka.http

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}

import scala.concurrent.duration._

case class EntityRestore(data: Long)

case class EntityWork(entityId: Long, data: Long)

case object Stop

object EntityActor {

  private val shardRegion = "EntityActor"

  def startClusterSharding(implicit system: ActorSystem): ActorRef = {
    ClusterSharding(system).start(
      typeName = shardRegion,
      entityProps = Props[EntityActor],
      settings = ClusterShardingSettings(system).withRole(None),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId)
  }

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case work@EntityWork(id, _) ⇒ (id.toString, work)
  }

  val numberOfShards = 40

  val extractShardId: ShardRegion.ExtractShardId = {
    case EntityWork(id, _) ⇒ (id % numberOfShards).toString
    case ShardRegion.StartEntity(id) ⇒
      // StartEntity is used by remembering entities feature
      (id.hashCode % numberOfShards).toString
  }
}

class EntityActor extends Actor with ActorLogging {

  context.setReceiveTimeout(20.seconds)

  var data: Long = -1L

  def update(newData: Long): Unit = {
    data = newData
  }

//  override def receiveRecover: Receive = {
//    case EntityRestore(d) =>
//      log.info("Recovering entity: " + self.path.name)
//      update(d)
//  }
//
//  override def receiveCommand: Receive = {
//    case EntityWork(_, d) =>
//      //update(d)
//      val msg = s"${self.path.name} was $data, now working on $d"
//      log.info(msg)
//      context.actorSelection("akka.tcp://" + context.system.name + "@localhost:2551/user/wsUi") ! msg
//    case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
//    case Stop => context.stop(self)
//
//    case unknown => log.error(s"Don't know what to do with: $unknown")
//  }
//
//  override def persistenceId: String = self.path.name

  override def receive: Receive = {
    case EntityWork(_, d) =>
      val msg = s"${self.path.name} was $data, now working on $d"
      update(d)
      log.info(msg)
      context.actorSelection("akka.tcp://" + context.system.name + "@localhost:2551/user/wsUi") ! msg

    case unknown => log.error(s"Don't know what to do with: $unknown")
  }
}
