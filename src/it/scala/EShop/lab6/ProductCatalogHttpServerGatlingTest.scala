package EShop.lab6
import io.gatling.core.Predef.{Simulation, StringBody, rampUsers, scenario, _}
import io.gatling.http.Predef.http

import scala.concurrent.duration._
import scala.util.Random
class ProductCatalogHttpServerGatlingTest extends Simulation {
  val httpProtocol = http
//    .baseUrls("http://localhost:8090")
    .baseUrls("http://localhost:9001", "http://localhost:9001", "http://localhost:9001")
    .acceptHeader("application/json")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  def random = Random.nextInt(5)

  def request = {
    http("filter product catalog")
      .post("/query")
      .body(StringBody {
        """
          |{
          | "brand": "gerber",
          | "productKeyWords": ["cream"]
          |}
          |""".stripMargin
      }).asJson
  }

  val scn = scenario("ProductCatalogHttpTest")
    .exec(request)
    .pause(random)
    .exec(request)
    .pause(random)
    .exec(request)
    .pause(random)
    .exec(request)

  setUp(
    scn.inject(rampUsers(100).during(10 seconds))
  ).protocols(httpProtocol)
}
