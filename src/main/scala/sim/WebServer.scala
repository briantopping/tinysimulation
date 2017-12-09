package sim

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ExceptionHandler, HttpApp, Route}
import akka.http.scaladsl.settings.ServerSettings
import akka.pattern.ask
import akka.util.Timeout
import spray.json._

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

final case class Request(url: String)
final case class Response(url: String, nodeID: Int)

class WebServer(workerCount: Int) extends HttpApp with SprayJsonSupport with Actor with ActorLogging {
  private var workers = Vector.empty[ActorRef]

  val handler = ExceptionHandler {
    case e: IllegalArgumentException  => extractUri { uri =>
      log.warning(s"Request for $uri could not be handled due to parsing error")
      complete(StatusCodes.BadRequest, "Parsing error")
    }
    case e: IndexOutOfBoundsException => complete(StatusCodes.NotFound, "Shortened URL not found")
    case e: Exception                 => extractUri { uri =>
      log.warning(s"Request for $uri could not be handled due to unknown error: $e")
      complete(StatusCodes.InternalServerError, "Unknown error")
    }
  }

  override def routes: Route = {
    import DefaultJsonProtocol._

    implicit val requestFormat = jsonFormat1(Request)
    implicit val responseFormat = jsonFormat2(Response)
    implicit val timeout: Timeout = Timeout(2.seconds)

    def handleGetUrlRequest(req: Request)(completion: Response => Route): Route = {
      WebServer.splitter(req.url) match {
        case Success((nodeID, sequence)) =>
          onSuccess(workers(nodeID) ? GetUrl(sequence)) {
            case Success(r: Response) => completion(r)
            case Failure(t)           => failWith(t)
          }
        case Failure(t)                  => failWith(t)
      }
    }

    handleExceptions(handler) {
      pathPrefix("urlshortener") {
        get {
          path(Segment) { s =>
            handleGetUrlRequest(Request(s)) { res =>
              redirect(res.url, StatusCodes.TemporaryRedirect)
            }
          } ~
          pathEnd {
            entity(as[Request]) { req =>
              handleGetUrlRequest(req) { res =>
                complete(res)
              }
            }
          }
        } ~
        post {
          entity(as[Request]) { req =>
            onSuccess(workers(select(req.url)) ? PostUrl(req.url)) {
              case Success(r: Response) => complete(r)
              case Failure(t)           => failWith(t)
            }
          }
        }
      }
    }
  }

  def select(url: String): Int = Math.abs(url.hashCode) % workerCount

  override def receive: Receive = Actor.emptyBehavior

  override def preStart(): Unit = {
    for (i <- 0 until workerCount) {
      workers = workers :+ context.actorOf(StorageWorker.props(i))
    }

    startServer("localhost", 8080, ServerSettings(context.system.settings.config), context.system)
    context.system.terminate()
  }
}

object WebServer extends App {
  private val nodeCount = 10
  // FIXME: this pattern is weak
  private val pattern = """.*(\d+)-(\d+)$""".r

  val system = ActorSystem()
  system.actorOf(WebServer.props(nodeCount))

  def splitter(key: String): Try[(Int, Int)] = key match {
    case pattern(node, seq) => Success((Integer.parseInt(node), Integer.parseInt(seq)))
    case _                  => Failure(new IllegalArgumentException("Failed to parse ID"))
  }

  def shortener(nodeID: Int, seq: Int): String = s"http://127.0.0.1/$nodeID-$seq"

  def props(workers: Int): Props = Props(new WebServer(workers))
}