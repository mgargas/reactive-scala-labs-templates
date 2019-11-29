package EShop.lab6

import EShop.lab5.{ProductCatalog, SearchService}
import EShop.lab5.ProductCatalog.{GetItems, Items}
import akka.actor.ActorSystem
import akka.cluster.routing.{ClusterRouterPool, ClusterRouterPoolSettings}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, OK}
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}

object ProductCatalogNodeApp extends App {
  private val config = ConfigFactory.load()

  val system = ActorSystem(
    "ClusterProductCatalog",
    config
      .getConfig(Try(args(0)).getOrElse("product-catalog-cluster"))
      .withFallback(config.getConfig("product-catalog-cluster"))
  )

}
class ProductCatalogClusterHttpServer(system: ActorSystem) extends HttpApp with SprayJsonSupport with JsonSupport {

  implicit val getItmesFormat   = jsonFormat2(ProductCatalog.GetItems)
  implicit val itemFormat       = jsonFormat5(ProductCatalog.Item)
  implicit val itemsFormat      = jsonFormat1(ProductCatalog.Items)
  implicit val timeout: Timeout = 5.seconds
  private val workers = system.actorOf(
    ClusterRouterPool(
      RoundRobinPool(0),
      ClusterRouterPoolSettings(totalInstances = 100, maxInstancesPerNode = 3, allowLocalRoutees = false)
    ).props(ProductCatalog.props(new SearchService())),
    name = "productCatalogRouter" + Random.nextInt(100)
  )

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

object ProductCatalogClusterHttpServer extends App {
  private val config          = ConfigFactory.load()
  private val system          = ActorSystem("ClusterProductCatalog", config.getConfig("product-catalog-cluster"))
  private val address: String = "localhost"
  private val port: Int       = args(0).toInt
  private val productCatalogSystem = ActorSystem(
    "ProductCatalog",
    config.getConfig("productcatalog").withFallback(config)
  )
  new ProductCatalogHttpServer(system, productCatalogSystem).startServer(address, port)
}
