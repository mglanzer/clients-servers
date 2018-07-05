package mpg.servers.akka.http

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import spray.json._
import DefaultJsonProtocol._
import akka.cluster.Cluster

import scala.concurrent.duration._

case class EntityWork(entityId: Long, data: Long)

case class EntityReport(entityId: String, node: String, data: Long)

object EntityActor {

  implicit val entityReportFormat: RootJsonFormat[EntityReport] = jsonFormat3(EntityReport)

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

class EntityActor extends Actor with WsUiSingletonSupport with ActorLogging {

  import EntityActor.entityReportFormat

  context.setReceiveTimeout(20.seconds)

  val cluster = Cluster(context.system)

  var data: Long = -1L

  def update(newData: Long): Unit = {
    data = newData
  }

  override def receive: Receive = {
    case EntityWork(_, d) =>
      update(d)
      log.info(s"${self.path.name} was $data, now working on $d")
      wsUiProxy ! EntityReport(
        self.path.name,
        cluster.selfAddress.toString,
        data)
        .toJson.compactPrint
  }
}
