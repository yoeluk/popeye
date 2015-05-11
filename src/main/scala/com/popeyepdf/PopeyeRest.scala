package com.popeyepdf

import akka.actor._
import spray.routing._
import spray.http.StatusCodes
import spray.routing.RequestContext
import akka.util.Timeout
import scala.util.Failure
import scala.concurrent.duration._

class PopeyeRest extends HttpServiceActor with PopeyeHttp {
  def receive = runRoute(popeyeRoutes)
}

trait PopeyeParserCreator { this: Actor =>
  def createPopeyeParser: ActorRef = context.actorOf(Props[PopeyeParser], "requester")
}

trait PopeyeHttp extends HttpService with PopeyeParserCreator { this: Actor =>

  import context._
  implicit val timeout = Timeout(60 seconds)
  import akka.pattern.ask
  import akka.pattern.pipe

  val popeyeParser = createPopeyeParser

  def popeyeRoutes: Route =
    path(Segment) { segment =>
      get { requestContext =>
        val request = ParseRequest(fileName = segment)
        val responder = createResponder(requestContext)
        popeyeParser ? request pipeTo responder
      }
    }

  def createResponder(requestContext: RequestContext) = {
    context.actorOf(Props(new ResponderSync(requestContext)))
  }
}

class ResponderSync(requestContext: RequestContext) extends Actor with ActorLogging {

  import spray.httpx.SprayJsonSupport._
  import PopeyeJsonProtocol._

  context.setReceiveTimeout(60 seconds)

  def receive = {
    case result: Result =>
      requestContext.complete(StatusCodes.OK, result)
      self ! PoisonPill
    case error: FailedResult =>
      requestContext.complete(StatusCodes.BadRequest, error)
      self ! PoisonPill
    case Failure(e) =>
      requestContext.complete(StatusCodes.BadRequest, e.getMessage)
      self ! PoisonPill
    case error:InternalError =>
      requestContext.complete(StatusCodes.InternalServerError, error)
      self ! PoisonPill
    case ReceiveTimeout =>
      context.setReceiveTimeout(Duration.Undefined)
      self ! PoisonPill
  }
}