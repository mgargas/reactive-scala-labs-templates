package EShop.lab6

import EShop.lab5.{ProductCatalog, SearchService}
import EShop.lab5.ProductCatalog.{GetItems, Items}
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, OK}
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success}
class ProductCatalogHttpServer(system: ActorSystem, productCatalogSystem: ActorSystem)
  extends HttpApp
  with SprayJsonSupport
  with JsonSupport {

  implicit val getItmesFormat   = jsonFormat2(ProductCatalog.GetItems)
  implicit val itemFormat       = jsonFormat5(ProductCatalog.Item)
  implicit val itemsFormat      = jsonFormat1(ProductCatalog.Items)
  implicit val timeout: Timeout = 5.seconds
  private val workers           = productCatalogSystem.actorOf(RoundRobinPool(9).props(ProductCatalog.props(new SearchService())))

  override protected def routes: Route = pathPrefix("query") {
    post {
      pathEnd {
        entity(as[GetItems]) {
          getItems
        }
      }
    }
  }

  private def getItems(request: GetItems): Route = {
    onComplete((workers ? request).mapTo[Items]) {
      case Failure(exception) =>
        system.log.error(exception.getMessage)
        complete(InternalServerError)
      case Success(items) => complete(OK, items)
    }
  }
}

object ProductCatalogHttpServer extends App {
  private val config          = ConfigFactory.load()
  private val system          = ActorSystem("ProductCatalogHttp", config.getConfig("catalogserver").withFallback(config))
  private val address: String = config.getString("catalogserver.address")
  private val port: Int       = config.getInt("catalogserver.port")
  private val productCatalogSystem = ActorSystem(
    "ProductCatalog",
    config.getConfig("productcatalog").withFallback(config)
  )
  new ProductCatalogHttpServer(system, productCatalogSystem).startServer(address, port)
}
