package sim

import akka.actor.{Actor, ActorLogging, Props}

import scala.collection.mutable
import scala.util.{Failure, Success}

final case class PostUrl(url: String)
final case class GetUrl(seq: Int)

class StorageWorker(nodeID: Int) extends Actor with ActorLogging {
  private val storage = mutable.LinkedHashSet.empty[String]

  override def receive: Receive = {
    case GetUrl(seq) => try {
      sender ! Success(Response(storage.toIndexedSeq(seq), nodeID))
    } catch {
      case e: Exception => sender ! Failure(e)
    }
    case r: PostUrl  => try {
      val seq = storage.add(r.url) match {
        case true  => storage.size - 1
        case false => storage.toIndexedSeq.indexOf(r.url)
      }
      sender ! Success(Response(WebServer.shortener(nodeID, seq), nodeID))
    } catch {
      case e: Exception => sender ! Failure(e)
    }
  }
}

object StorageWorker {
  def props(id: Int): Props = Props(new StorageWorker(id))
}