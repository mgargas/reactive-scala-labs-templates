package EShop.lab5

import EShop.lab5.PaymentService.{PaymentClientError, PaymentServerError, PaymentSucceeded}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import akka.event.LoggingReceive
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.pattern.PipeToSupport

object PaymentService {

  def props(method: String, payment: ActorRef) = Props(new PaymentService(method, payment))

  class PaymentClientError extends Exception // http statuses 400, 404
  class PaymentServerError extends Exception // http statuses 500, 408, 418

  case object PaymentSucceeded // http status 200

}

class PaymentService(method: String, payment: ActorRef) extends Actor with ActorLogging with PipeToSupport {
  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  import context.dispatcher
  private val http = Http(context.system)
  private val URI  = getURI

  override def preStart(): Unit =
    http.singleRequest(HttpRequest(uri = getURI)).pipeTo(self)

  private def getURI: String = method match {
    case "payu"   => "http://127.0.0.1:8080"
    case "paypal" => s"http://httpbin.org/status/408"
    case "visa"   => s"http://httpbin.org/status/200"
    case _        => s"http://httpbin.org/status/404"
  }

  override def receive: Receive = LoggingReceive.withLabel("[Payment service]") {
    case HttpResponse(StatusCodes.OK, _, _, _) =>
      payment ! PaymentSucceeded
    case HttpResponse(StatusCodes.InternalServerError, _, _, _) => throw new PaymentServerError
    case HttpResponse(StatusCodes.RequestTimeout, _, _, _)      => throw new PaymentServerError
    case HttpResponse(StatusCodes.ImATeapot, _, _, _)           => throw new PaymentServerError
    case HttpResponse(StatusCodes.BadRequest, _, _, _)          => throw new PaymentClientError
    case HttpResponse(StatusCodes.NotFound, _, _, _)            => throw new PaymentClientError
    case Status.Failure(exception)                              => throw exception
  }

}
