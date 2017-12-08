package sim

import akka.actor.{Actor, ActorLogging, Props}

import scala.util.{Failure, Success}

final case class PostUrl(url: String)
final case class GetUrl(seq: Int)

class StorageWorker(nodeID: Int) extends Actor with ActorLogging {
  var storage = Vector.empty[String]

  override def receive: Receive = {
    case GetUrl(seq) => try {
      sender ! Success(Response(storage(seq), nodeID))
    } catch {
      case e: Exception => sender ! Failure(e)
    }
    case r: PostUrl  => try {
      storage = storage :+ r.url
      sender ! Success(Response(WebServer.shortener(nodeID, storage.size - 1), nodeID))
    } catch {
      case e: Exception => sender ! Failure(e)
    }
  }
}

object StorageWorker {
  def props(id: Int): Props = Props(new StorageWorker(id))
}