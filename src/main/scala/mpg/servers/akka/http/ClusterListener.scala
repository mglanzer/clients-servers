package mpg.servers.akka.http

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import mpg.servers.akka.http.ClusterListener.AddListener

object ClusterListener {

  case class AddListener(ref: ActorRef)

}

class ClusterListener extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  var listeners: Seq[ActorRef] = Nil

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case AddListener(ref) => listeners = listeners :+ ref
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
      listeners.foreach(ref => ref ! s"Member Is Up: $member")
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
      listeners.foreach(ref => ref ! s"Member is unreachable: $member")
    case MemberRemoved(member, previousStatus) ⇒
      log.info(
        "Member is Removed: {} after {}",
        member.address, previousStatus)
      listeners.foreach(ref => ref ! s"Member removed: $member")
    case _: MemberEvent ⇒ // ignore
  }
}
